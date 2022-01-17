package org.folio.des.service.impl.aqcuisition;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.common.SshException;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.fs.SftpFileSystemProvider;
import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;
import org.folio.des.domain.dto.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static org.apache.sshd.sftp.common.SftpHelper.DEFAULT_SUBSTATUS_MESSAGE;
import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@Testcontainers
@SpringBootTest()
class SFTPDownloadFileServiceTest {

  @Autowired
  private SFTPDownloadFileService sftpDownloadFileService;

  private static final int PORT = 22;
  private static final String INVALID_HOST = "invalidhost123";
  private static final String USERNAME = "user";
  private static final String PASSWORD = "password";
  private static final String PASSWORD_INVALID = "dontLetMeIn";
  private static final String EXPORT_FOLDER_NAME = "upload";

  private static String SFTP_HOST;
  private static Integer MAPPED_PORT;

  @Container
  public static final GenericContainer sftp = new GenericContainer(
    new ImageFromDockerfile()
      .withDockerfileFromBuilder(builder ->
        builder
          .from("atmoz/sftp:latest")
          .run("mkdir -p " + File.separator + EXPORT_FOLDER_NAME + "; chmod -R 777 " + File.separator + EXPORT_FOLDER_NAME)
          .build()))
    .withExposedPorts(PORT)
    .withCommand(USERNAME + ":" + PASSWORD + ":::upload");

  @BeforeAll
  public static void staticSetup() {
    MAPPED_PORT = sftp.getMappedPort(PORT);
    SFTP_HOST = sftp.getHost();
  }

  @Test
  void testSuccessfullyLogin() throws IOException {
    log.info("=== Test successful login ===");
    SftpClient sftp = sftpDownloadFileService.getSftpClient(USERNAME, PASSWORD, SFTP_HOST, MAPPED_PORT);

    assertNotNull(sftp);

    sftpDownloadFileService.logout();
  }

  @Test
  void testFailedConnect() {
    log.info("=== Test unsuccessful login ===");
    Exception exception = assertThrows(SshException.class, () -> {
      sftpDownloadFileService.getSftpClient(USERNAME, PASSWORD, INVALID_HOST, MAPPED_PORT);
    });

    String expectedMessage = "Failed (UnresolvedAddressException) to execute";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));
  }

  @Test
  void testFailedLogin() {
    log.info("=== Test unsuccessful login ===");
    Exception exception = assertThrows(IOException.class, () -> sftpDownloadFileService.getSftpClient(USERNAME, PASSWORD_INVALID, SFTP_HOST, MAPPED_PORT));

    String expectedMessage = "SFTP server authentication failed";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));
  }

  //todo implements test
  void testSuccessfulDownload() throws IOException {
    log.info("=== Test successful download ===");

    Job job = new Job();
    ExportTypeSpecificParameters exportTypeSpecificParameters = new ExportTypeSpecificParameters();
    VendorEdiOrdersExportConfig vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();
    EdiFtp ediFtp = new EdiFtp();

    ediFtp.serverAddress(SFTP_HOST);
    ediFtp.setFtpPort(MAPPED_PORT);
    ediFtp.setUsername(USERNAME);
    ediFtp.setPassword(PASSWORD);
    ediFtp.setInvoiceDirectory(EXPORT_FOLDER_NAME + "/path/to/invoice/directory");
    ediFtp.setOrderDirectory(EXPORT_FOLDER_NAME + "/path/to/order/directory");

    vendorEdiOrdersExportConfig.setEdiFtp(ediFtp);

    exportTypeSpecificParameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);

    job.setId(UUID.randomUUID());
    job.setStartTime(new Date());
    job.setType(ExportType.INVOICE_EXPORT);
    job.setExportTypeSpecificParameters(exportTypeSpecificParameters);

    String fileName = String.format("%s_po_edi_export_%s.edi", job.getId(), job.getStartTime());

//    boolean uploaded = upload(ediFtp.getInvoiceDirectory(), fileName);
//    byte[] fileBytes = sftpDownloadFileService.download(job);

//    assertTrue(uploaded);
//    assertNotNull(fileBytes);

    sftpDownloadFileService.logout();
  }

  private boolean upload(String folder, String filename) throws IOException {
    String content = "Some string with content for download";
    SftpClient sftpClient = sftpDownloadFileService.getSftpClient(USERNAME, PASSWORD, SFTP_HOST, MAPPED_PORT);
    String folderPath = StringUtils.isEmpty(folder) ? "" : (folder + File.separator);
    String fileAbsPath = folderPath + filename;

    createRemoteDirectoryIfAbsent(sftpClient, folder);
    URI uri = SftpFileSystemProvider.createFileSystemURI(SFTP_HOST, MAPPED_PORT, USERNAME, PASSWORD);
    try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
      Path remotePath = fs.getPath(fileAbsPath);
      Files.createFile(remotePath);
      Files.write(remotePath, content.getBytes(StandardCharsets.UTF_8));
      log.info("successfully uploaded to SFTP: {}", fileAbsPath);
      return true;
    } catch (IOException e) {
      log.error(e);
    }
    return false;
  }

  private void createRemoteDirectoryIfAbsent(SftpClient sftpClient, String folder) throws IOException {
    if (isDirectoryAbsent(sftpClient, folder)) {
      String[] folders = folder.split("/");
      StringBuilder path = new StringBuilder(folders[0]).append("/");

      for (int i = 0; i < folders.length; i++) {
        if (isDirectoryAbsent(sftpClient, path.toString())) {
          sftpClient.mkdir(path.toString());
        }
        if (i == folders.length - 1) return;
        path.append(folders[i + 1]).append("/");
      }
      log.info("A directory has been created: {}", folder);
    }
  }

  private boolean isDirectoryAbsent(SftpClient sftpClient, String folder) throws IOException {
    try {
      sftpClient.open(folder, SftpClient.OpenMode.Read);
    } catch (SftpException sftpException) {
      if (DEFAULT_SUBSTATUS_MESSAGE.get(SftpConstants.SSH_FX_NO_SUCH_FILE).contains(sftpException.getMessage())) {
        return true;
      } else throw sftpException;
    }
    return false;
  }

}
