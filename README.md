# dataset-builder

This project is a web application for building a dataset before ML training.
By run the following steps you can filter your raw data, add a statistics features and transformer each column.

## Filters
One of the options is this app is to filter chosen rows by selecting a header with value and one of the filters types:
"equal to"), "grater than", "less than", "contains", "doesn't contain"

## Statistics
The next option is to add statistics for list of headers with target header to count.
You can choose if you want calculate sum function or average function.
The default days offset is 7 days before and 14 days before.
With process option you will get at the end of the process 2 new columns for each chosen statistics with the name of 
the relevant headers, the target function and number of days 7/14.


## Transformers
Here we have 2 options:
1) minMaxScale - normalize the column values between 0 and 1.
2) ordinalEncoder - make category features as number for example:
A, B, C, A -> 1, 2, 3, 1  


##How to run it? 
1) Install tomcat 9.0.* in your device.
2) Install mysql DB and JDBC driver.
3) Creat your own DB and put the user name and password as a json in resources
(put it with file name: config.json). After you have your own DB please change the JDBC_URL in utils.Constants.
4) Add users table into your DB with userName column and password.
5) Finally you can run the application.

