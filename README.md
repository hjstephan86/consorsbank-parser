## consorsbank-parser

This is a simple java project to parse monthly PDF reports genrated by Consorsbank. Notice, [pdfbox-app-3.0.3.jar](https://pdfbox.apache.org/3.0/commandline.html) is used to parse PDF reports. You can also use pdfbox-app-3.0.3.jar on your own as follows:

```java -jar pdfbox-app-3.0.3.jar export:text -i=2023/KONTOAUSZUG_GIROKONTO_XXXYYYZZZ_dat20231229_id1309133552.pdf```

This parser parses PDF reports and prints the result of each transfer in a pretty format to console. Additionally, it exports the transfers to CSV. Paths can be configured inside `App.Helper`, i.e., the path to the folder where the PDF reports to pasrse are located in and the path to the CSV which should be generated for export. Feel free to add any filter condition inside `App.printTransfers(ArrayList<Transfer> transfers)` for personal evaluations or simply import the generated CSV into a table calculation tool and apply filter conditions there.

Notice, any transfer inside the PDF is identified by one of the following transfer types: `GEHALT/RENTE|EURO-UEBERW.|LASTSCHRIFT|DAUERAUFTRAG|GIROCARD|GEBUEHREN`. If other types of transfers are required, add additional types to `Helper.REGEX_TRANSFER_TYPES`.
