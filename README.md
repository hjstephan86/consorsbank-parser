## consorsbank-parser

This is a simple java project to parse monthly PDF reports genrated by Consorsbank and DHL delivery receipts. Notice, [pdfbox](https://pdfbox.apache.org/3.0/commandline.html) is used to parse PDF reports and [mindee](https://platform.mindee.com) is used to parse delivery receipts.

This parser parses PDF reports and prints the data of each transfer in a pretty format to console. It also parses delivery receipts in JPG or PDF format and prints the data of each delivery receipt including the tracking id to console. It identifies retoure transfers to which you can assign tracking ids interactively via console. Additionally, it exports the transfers to CSV, respecting tracking id assignemnts by the user. The API key for mindee and the following paths can be configured inside `App.Helper` or passed as arguments, i.e., 
- the path to the folder where the PDF reports to pasrse are located in, 
- the path to the folder where the delivery receipts are located in, 
- the path to the CSV which should be generated,
- and, optionally, the path to the CSV which is checked for already assigned tracking ids (by a prior execution and CSV generation).

Feel free to add any filter condition inside `App.printTransfers(ArrayList<Transfer> transfers)` for personal evaluations or simply import the generated CSV into a table calculation tool and apply filter conditions there.

Notice, any transfer inside the PDF is identified by one of the following transfer types: `GEHALT/RENTE|EURO-UEBERW.|LASTSCHRIFT|DAUERAUFTRAG|GIROCARD|GEBUEHREN`. If other types of transfers are required, add additional types to `Helper.REGEX_TRANSFER_TYPES`.

Notice, this parser supports delivery receipts obtained from a DHL service point or from a DHL self-service point via mail (simply print the mail as PDF).
