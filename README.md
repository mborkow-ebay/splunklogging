## INSTALLATION
To install this Maven plugin, please run the following command:
```mvn clean compile package install
```


## CONFIGURATION
After installation, in order to generate CSV files for Splunk to consume add the following to your POM file in the build section:

    <build>
        <plugins>
            <plugin>
                <groupId>com.ebay.mobile.splunklogging.plugin</groupId>
                <artifactId>splunklogging-maven-plugin</artifactId>
                <version>1.0</version>
            </plugin>
	    ....
        </plugins>
    </build>

The plugin assumes the XML file it will be converting to CSV is named testng-results.xml and is located in target/surefire-reports.  The output CSV will be generated in the same directory and be named testng-
results.csv.  These three parameters can be configured within your POW with the following:
- "generate-csv.fileLocation"
- "generate-csv.outputFile"
- "generate-csv.inputFile"

## USAGE
Add the command line option "splunklogging:generate-csv" when running Maven.  For example:
mvn failsafe:integration-test splunklogging:generate-csv

Your CSV will be generated and, if Splunk is monitoring that file, automatically indexed

## LICENSE
License - Apache 2: http://www.apache.org/licenses/LICENSE-2.0
