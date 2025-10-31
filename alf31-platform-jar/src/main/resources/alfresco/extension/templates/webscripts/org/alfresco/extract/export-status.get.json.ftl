<#escape x as jsonUtils.encodeJSONString(x)>
{
  "jobId": "${jobId!""}",
  "status": "${status!""}",
  "maxDocs": ${maxDocs!0?c},
  "extractedCount": ${extractedCount!0?c},
  "progress": ${progress!0?c},
  "message": "${message!""}",
  "keywords": "${keywords!""}",
  "mimetype": "${mimetype!""}",
  "extractionPath": "${extractionPath!""}",
  "startTime": <#if startTime??>"${startTime?datetime?iso_utc}"<#else>null</#if>,
  "endTime": <#if endTime??>"${endTime?datetime?iso_utc}"<#else>null</#if>,
  "duration": ${duration!0?c}<#if error??>,
  "error": "${error}"</#if>
}
</#escape>
