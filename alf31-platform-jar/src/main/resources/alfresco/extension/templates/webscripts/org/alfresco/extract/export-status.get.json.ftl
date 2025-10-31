<#import "/org/alfresco/repository/generic-paged-results.lib.ftl" as gen/>
{
  "jobId": <@gen.jsonstring value=jobId!"" />,
  "status": <@gen.jsonstring value=status!"" />,
  "maxDocs": ${maxDocs!0?c},
  "extractedCount": ${extractedCount!0?c},
  "progress": ${progress!0?c},
  "message": <@gen.jsonstring value=message!"" />,
  "keywords": <@gen.jsonstring value=keywords!"" />,
  "mimetype": <@gen.jsonstring value=mimetype!"" />,
  "extractionPath": <@gen.jsonstring value=extractionPath!"" />,
  "startTime": <#if startTime??>"${startTime?datetime?iso_utc}"<#else>null</#if>,
  "endTime": <#if endTime??>"${endTime?datetime?iso_utc}"<#else>null</#if>,
  "duration": ${duration!0?c}<#if error??>,
  "error": <@gen.jsonstring value=error!"" /></#if>
}
