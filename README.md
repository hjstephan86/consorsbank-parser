## consorsbank-parser

This is a simple java project to parse monthly PDF reports genrated by Consorsbank and DHL delivery receipts. Notice, [pdfbox](https://pdfbox.apache.org/3.0/commandline.html) is used to parse PDF reports and the [mindee](https://platform.mindee.com) API is used to parse delivery receipts.

This parser parses PDF reports and prints the data of each transfer in a pretty format to console. It also parses delivery receipts in JPG or PDF format and prints the data of each delivery receipt including the tracking id to console. It identifies retoure transfers to which you can assign tracking ids interactively via console. Additionally, it exports the transfers to CSV, respecting tracking id assignemnts by the user. The mindee API endpoint data (key, endpoint name, account name, version) and the following paths can be configured inside `App.Helper` or passed as command line arguments, i.e., 
- the path to the folder where the PDF reports to pasrse are located in, 
- the path to the folder where the delivery receipts are located in, 
- the path to the CSV containing transfers which should be generated (for testing purposes),
- the path to the CSV containing transfers which are checked for already assigned tracking ids (by a prior execution and CSV generation),
- the path to the CSV containing delivery receipts which should be used for tracking id assignment. Notice, not assigned tracking ids in a prior execution are generated into a CSV and can be reused for a later tracking id assignment (to save mindee API calls),
- the path to the CSV containting delivery receipts with tracking ids that were not assigned (for testing purposes).

Feel free to add any filter condition inside `App.printTransfers(ArrayList<Transfer> transfers)` for personal evaluations or simply import the generated CSV into a table calculation tool and apply filter conditions there.

Notice, any transfer inside the PDF is identified by one of the following transfer types: `GEHALT/RENTE|EURO-UEBERW.|LASTSCHRIFT|DAUERAUFTRAG|GIROCARD|GEBUEHREN`. If other types of transfers are required, add additional types to `Helper.REGEX_TRANSFER_TYPES`.

Notice, this parser supports delivery receipts obtained from a DHL service point or from a DHL self-service point via mail (simply print the mail as PDF).
