package org.folio.des.converter.aqcuisition;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.scheduling.acquisition.AcqBaseExportTaskTrigger;
import org.folio.des.scheduling.base.ExportTaskTrigger;
import org.folio.des.validator.acquisition.EdifactOrdersExportParametersValidator;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.*;

@AllArgsConstructor
@Log4j2
@Service
public class EdifactOrdersExportConfigToTaskTriggerConverter implements Converter<ExportConfig, List<ExportTaskTrigger>>  {
  private EdifactOrdersExportParametersValidator validator;

  @Override
  public List<ExportTaskTrigger> convert(ExportConfig exportConfig) {
    ExportTypeSpecificParameters specificParameters = exportConfig.getExportTypeSpecificParameters();
    Errors errors = new BeanPropertyBindingResult(specificParameters, "specificParameters");
    validator.validate(specificParameters, errors);

    List<ExportTaskTrigger> exportTaskTriggers = new ArrayList<>(1);
    Optional.ofNullable(specificParameters.getVendorEdiOrdersExportConfig())
      .map(VendorEdiOrdersExportConfig::getEdiSchedule)
      .ifPresent(ediSchedule -> {
        ScheduleParameters scheduleParams = ediSchedule.getScheduleParameters();
        if (isScheduleTimePresent(scheduleParams)) {
          if (scheduleParams.getId() == null) {
            scheduleParams.setId(UUID.fromString(exportConfig.getId()));
          }
          scheduleParams.setTimeZone(scheduleParams.getTimeZone());
          var trigger = new AcqBaseExportTaskTrigger(scheduleParams, null, ediSchedule.getEnableScheduledExport());
          exportTaskTriggers.add(trigger);
        }
    });
    return exportTaskTriggers;
  }

  private boolean isScheduleTimePresent(ScheduleParameters params) {
    return !Objects.isNull(params) && !Objects.isNull(params.getScheduleTime());
  }
}
