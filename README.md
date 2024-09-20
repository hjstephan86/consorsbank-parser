## consorsbank-parser

This is a simple java project to parse monthly PDF reports genrated by Consorsbank and DHL delivery receipts. Notice, [pdfbox](https://pdfbox.apache.org/3.0/commandline.html) is used to parse PDF reports and the [mindee](https://platform.mindee.com) API is used to parse delivery receipts.

This parser parses PDF reports and prints the data of each transfer in a pretty format to console. It also parses delivery receipts in JPG or PDF format and prints the data of each delivery receipt including the tracking id to console. It searches for retoure transfers to which you can assign tracking ids interactively via console. Additionally, it exports the transfers to CSV, respecting tracking id assignemnts by the user. The mindee API endpoint data (key, endpoint name, account name, version) and the following paths can be configured inside `com.consorsbank.parser.Helper` or, when running this project as jar, passed as command line arguments, i.e., 
1. the path to the folder where the PDF reports to pasrse are located in, e.g., `/home/user/Downloads/transfers/`,
2. the path to the folder where the delivery receipts are located in, e.g., `/home/user/Downloads/receipts/`,
3. the path to the CSV containing transfers which should be generated, e.g., `/home/user/Downloads/transfers/Transfers-%DATETIME%.csv"` (you can use the `%DATETIME%` macro for a current timestamp),
4. the path to the CSV containing transfers which are checked for already assigned tracking ids (by a prior execution and CSV generation), e.g., `/home/user/Downloads/transfers/Transfers-2024-09-22_10-55-49.csv`,
5. the path to the CSV containing delivery receipts which should be generated and used for a later tracking id assignment, e.g., `/home/user/Downloads/Receipts-%DATETIME%.csv`. Notice, not assigned tracking ids are generated into this CSV and can be reused for later tracking id assignments to save mindee API calls,
6. the path to the CSV containting delivery receipts with tracking ids that were not assigned, e.g., `/home/user/Downloads/receipts/Receipts-2024-09-22_10-55-49.csv`.

For the first execution of the project you can simply use an empty file for arguments 4. and 6.

Notice, the retoure transfer assignment is respecting 1:1, 1:n, and n:m retoure assignments. For n:m retoure assignments it is first checked whether a best fit packaging is possible with the chronological order given by the transfers. If this order does not work, a best fit packaging is performed where the transfers are ordered in ascending order by the balance value of each transfer.

Feel free to add any filter condition inside `App.printTransfers(ArrayList<Transfer> transfers)` for personal evaluations or simply import the generated CSV into a table calculation tool and apply filter conditions there.

Notice, any transfer inside the PDF is identified by one of the following transfer types: `GEHALT/RENTE|EURO-UEBERW.|LASTSCHRIFT|DAUERAUFTRAG|GIROCARD|GEBUEHREN`. If other types of transfers are required, add additional types to `Helper.REGEX_TRANSFER_TYPES`.

This parser supports delivery receipts obtained from a DHL service point or from a DHL self-service point via mail (simply print the mail as PDF).