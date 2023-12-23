package org.folio.des.service.bursarlegacy;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.folio.de.entity.bursarlegacy.JobWithLegacyBursarParameters;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.JobWithLegacyBursarParametersCollection;
import org.folio.des.repository.CQLService;
import org.folio.des.repository.bursarlegacy.BursarExportLegacyJobRepository;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class BursarExportLegacyJobServiceTest {

  @Mock
  private BursarExportLegacyJobRepository repository;

  @Mock
  private CQLService cqlService;

  @Test
  void testGetBlankQuery() {
    List<JobWithLegacyBursarParameters> legacyJobs = new ArrayList<>();
    JobWithLegacyBursarParameters legacyJob1 = new JobWithLegacyBursarParameters();
    legacyJob1.setId(UUID.fromString("0000-00-00-00-000000"));
    legacyJobs.add(legacyJob1);
    JobWithLegacyBursarParameters legacyJob2 = new JobWithLegacyBursarParameters();
    legacyJob2.setId(UUID.fromString("0000-00-00-00-000001"));
    legacyJob2.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    legacyJobs.add(legacyJob2);

    Page<JobWithLegacyBursarParameters> page = new PageImpl<JobWithLegacyBursarParameters>(
      legacyJobs,
      new OffsetRequest(0, 1),
      1L
    );

    when(repository.findAll(new OffsetRequest(0, 2))).thenReturn(page);
    BursarExportLegacyJobService service = new BursarExportLegacyJobService(
      repository,
      cqlService
    );

    JobWithLegacyBursarParametersCollection legacyJobCollection = service.get(0, 2, "");

    assertEquals(2, legacyJobCollection.getJobRecords().size());
    assertEquals(
      UUID.fromString("0000-00-00-00-000000"),
      legacyJobCollection.getJobRecords().get(0).getId()
    );
  }
}
