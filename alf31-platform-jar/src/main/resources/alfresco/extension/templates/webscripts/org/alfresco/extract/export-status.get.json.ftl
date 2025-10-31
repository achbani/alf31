{
  "jobId": "${(jobId!"")?json_string}",
  "status": "${(status!"")?json_string}",
  "maxDocs": ${maxDocs!0?c},
  "extractedCount": ${extractedCount!0?c},
  "progress": ${progress!0?c},
  "message": "${(message!"")?json_string}",
  "keywords": "${(keywords!"")?json_string}",
  "mimetype": "${(mimetype!"")?json_string}",
  "extractionPath": "${(extractionPath!"")?json_string}",
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
  "duration": ${duration!0?c}<#if error??>,
  "error": "${error?json_string}"</#if>
}
