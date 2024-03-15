

# eu-subsidy-compliance-stub

eu-subsidy-compliance-stub provides test data for eu-subsidy-compliance-frontend when run locally.
The details for the test EORI data is as shown below:

## EORI Test data

| Action                   | EORI(ends with) | Response Error Code | Response Error text                                           |
|--------------------------|-----------------|---------------------|---------------------------------------------------------------
| Create Undertaking       | `...999`        | 500                 | Internal server error                                         | 
|                          | `...888`        | 004                 | Duplicate submission acknowledgment reference                 |  
|                          | `...777`        | 101                 | EORI already associated with another Undertaking              |  
|                          | `...666`        | 102                 | Invalid EORI number                                           |  
|                          | `...555`        | 113                 | Postcode missing for the address                              |  
| Retrieve Undertaking     | `...999`        | 500                 | Internal server error                                         | 
|                          | `...888`        | 107                 | Undertaking reference in the API not Subscribed in ETMP       |  
|                          | `...777`        | 055                 | ID number missing or invalid                                  |  
|                          | `...511`        | 200                 | Return an undertaking with a status of 'suspendedAutomated'   |  
|                          | `...316`        | 200                 | Return an undertaking lead with a status of 'suspendedManual' |  
| Amend Undertaking Member | `...999`        | 500                 | Internal server error                                         | 
|                          | `...888`        | 004                 | Duplicate submission acknowledgment reference                 |  
|                          | `...777`        | 106                 | EORI not Subscribed in ETMP                                   |  
|                          | `...666`        | 107                 | Undertaking reference in the API not Subscribed in ETMP       |  
|                          | `...555`        | 108                 | Relationship with another undertaking exist for EORI          |  
|                          | `...444`        | 109                 | Relationship does not exist for EORI                          |  
|                          | `...333`        | 110                 | Subsidy Compliance address does not exist for EORI            | 
| Update Undertaking       | `...999`        | 500                 | Internal server error                                         | 
|                          | `...888`        | 004                 | Duplicate submission acknowledgment reference                 |  
|                          | `...777`        | 116                 | Invalid Undertaking ID                                        |  
| Update Subsidy Usage     | `...999`        | 500                 | Internal server error                                         | 
|                          | `...888`        | 004                 | Duplicate submission acknowledgment reference                 |  
|                          | `...777`        | 107                 | Undertaking reference in the API not Subscribed in ETMP       |  
|                          | `...666`        | 106                 | EORI not Subscribed in ETMP                                   |  
|                          | `...555`        | 112                 | EORI $eori not linked with undertaking.                       | 
|                          | `...444`        | 111                 | Subsidy allocation ID number or date is invalid               | 
| Retrieve Subsidy Usage   | `...999`        | 500                 | Internal server error                                         | 
|                          | `...888`        | 004                 | Duplicate submission acknowledgment reference                 |  
|                          | `...777`        | 201                 | Invalid Undertaking identifier                                |  
|                          | `...666`        | 202                 | Error while fetching the Currency conversion values           |
| Get Undertaking Balance  | `...111908`     | 500                 | Undertaking doesn't exist                                     | 

### Launching the service locally

To bring up the service on the configured port `9095`, use

```
sbt run
```
### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
