## INSTALLATION
To install this Maven plugin, please run the following command:

    mvn clean compile package install


## CONFIGURATION
After installation, in order to generate JSON files for Splunk to consume add the following to your POM file in the build section:

    <build>
        <plugins>
            <plugin>
                <groupId>com.ebay.mobile.splunklogging.plugin</groupId>
                <artifactId>splunklogging-maven-plugin</artifactId>
                <version>2.0</version>
            </plugin>
	    ....
        </plugins>
    </build>

The plugin assumes the XML file it will be converting to JSON is named testng-results.xml and is located in target/surefire-reports.  The output JSON will be generated in the same directory and be named testng-
results.json.  These three parameters can also be configured in your POM, in the plugin's block, with the following:

    <configuration>
        <fileLocation>location/on/disk</fileLocation>
		<inputFile>test-results.xml</inputFile>
		<outputFile>splunk-output.json</outputFile>
    </configuration>

## USAGE
Add the command line option "splunklogging:generate-json" when running Maven.  For example:

    mvn failsafe:integration-test splunklogging:generate-json

Your JSON will be generated and, if Splunk is monitoring that file, automatically indexed

## LICENSE
License - Apache 2: http://www.apache.org/licenses/LICENSE-2.0
