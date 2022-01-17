package org.folio.des.service.impl.aqcuisition;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.folio.des.domain.dto.EdiFtp;
import org.folio.des.domain.dto.Job;
import org.folio.des.service.DownloadFileService;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Log4j2
@Repository
@RequiredArgsConstructor
public class SFTPDownloadFileService implements DownloadFileService {

  private SshSimpleClient sshClient;
  private static final int LOGIN_TIMEOUT_SECONDS = 5;

  public SftpClient getSftpClient(String username, String password, String host, int port) throws IOException {
    sshClient = new SshSimpleClient(username, password, host, port);
    sshClient.startClient();
    ClientSession clientSession = sshClient.connect();

    AuthFuture auth = clientSession.auth();
    auth.await(LOGIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    if (auth.isSuccess()) {
      log.info("authentication successful: {}", auth.isSuccess());
      return createSftpClient(clientSession);
    } else {
      clientSession.close();
      sshClient.stopClient();
      throw new IOException("SFTP server authentication failed");
    }
  }

  private SftpClient createSftpClient(ClientSession session) throws IOException {
    return SftpClientFactory.instance().createSftpClient(session);
  }

  @Override
  public byte[] download(Job job, String path) {
    try {
      EdiFtp ediFtp = job.getExportTypeSpecificParameters().getVendorEdiOrdersExportConfig().getEdiFtp();
      SftpClient sftpClient = getSftpClient(ediFtp.getUsername(), ediFtp.getPassword(), ediFtp.getServerAddress(), ediFtp.getFtpPort());
      //todo leave only folders
      return download(sftpClient, path);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private byte[] download(SftpClient sftpClient, String path) {
    byte[] fileBytes = null;
    try {
      InputStream stream = sftpClient.read(path);
      log.info("File found from path: {}", path);
      fileBytes = stream.readAllBytes();
      stream.close();
    } catch (IOException e) {
      log.error(e);
    }
    return fileBytes;
  }

  public void logout() {
    if (sshClient.getSshClient().isStarted()) {
      sshClient.stopClient();
    }
  }

}
