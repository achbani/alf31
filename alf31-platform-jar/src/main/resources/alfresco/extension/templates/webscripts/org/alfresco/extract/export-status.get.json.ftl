{
  "jobId": "${jobId!""}",
  "status": "${status!""}",
  "maxDocs": ${maxDocs!0},
  "extractedCount": ${extractedCount!0},
  "progress": ${progress!0},
  "message": "${message!""}",
  "keywords": "${keywords!""}",
  "mimetype": "${mimetype!""}",
  "extractionPath": "${extractionPath!""}",
  <#if startTime??>
  "startTime": "${startTime?datetime?iso_utc}",
  <#else>
  "startTime": null,
  </#if>
  <#if endTime??>
  "endTime": "${endTime?datetime?iso_utc}",
  <#else>
  "endTime": null,
  </#if>
  "duration": ${duration!0}
  <#if error??>
  ,"error": "${error}"
  </#if>
}
