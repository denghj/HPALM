# HPALM -Mod
HP Alm Java Rest API Client
Changed from Urlopenconenctions to using httpClient to avoid certificate error we got when using testNG listeners

Default HP code has issue, which is throwing 401 exception. This code fixes that issue.

Official code is at: http://alm-help.saas.hpe.com/en/12.53/api_refs/REST_TECH_PREVIEW/ALM_REST_API_TP.html
