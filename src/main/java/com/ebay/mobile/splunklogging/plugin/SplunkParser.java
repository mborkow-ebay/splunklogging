package com.ebay.mobile.splunklogging.plugin;

import javax.xml.parsers.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import java.io.*;
import org.testng.annotations.Test;
import org.testng.annotations.Parameters;
import java.util.List;
import java.util.ArrayList;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo( name = "generate-csv")
public class SplunkParser extends AbstractMojo {
    
    String fs = System.getProperty("file.separator");
    String className;
    String reporterTextString = "";
    String exceptionName = "";
    String exceptionMessage = "";
    String parameterString = "";
    List<String> theOutput;
    @Parameter(property = "generate-csv.fileLocation", defaultValue = "target/surefire-reports")
    String fileLocation;
    @Parameter(property = "generate-csv.outputFile", defaultValue = "testng-results.csv")
    String outputFile;
    @Parameter(property = "generate-csv.inputFile", defaultValue = "testng-results.xml")
    String inputFile;
    
    public SplunkParser () {}
    
    public SplunkParser (String fileLocation, String inputFile, String outputFile) {
        this.fileLocation = fileLocation;
        this.outputFile = outputFile;
        this.inputFile = inputFile;
    }
    
    private void parseXmlResponse (String xml) throws Exception {
        
        NodeList nl = getResponseNodes(xml);
        Node aNode;
        for (int i = 0; i < nl.getLength(); ++i) {
            aNode = nl.item(i);
            if (aNode.getNodeType() == Node.ELEMENT_NODE)
                parseNode(aNode);
        }
    }
    
    private NodeList getResponseNodes (String xmlText) throws Exception {
        
        ByteArrayInputStream bais = new ByteArrayInputStream(xmlText.getBytes("UTF-8"));
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(bais);
        
        Element e = doc.getDocumentElement();
        NodeList nl = e.getChildNodes();
        return nl;
    }
    
    private void clearPreviousTestData () {
        
        parameterString = "";
        exceptionName = "";
        exceptionMessage = "";
        reporterTextString = "";
    }
    
    private void parseNode (Node n) {
        
        NodeList childNodes = n.getChildNodes();
        Node aNode;
        for (int i = 0; i < childNodes.getLength(); ++i) {
            aNode = childNodes.item(i);
            String aName = aNode.getNodeName().trim();
            if (aNode.getNodeType() == Node.ELEMENT_NODE ) {
                if (aName.equalsIgnoreCase("test-method")) {
                    // since we are in a new test-method block we can clear all our old data...
                    StringBuffer buf = new StringBuffer();
                    clearPreviousTestData();
                    NamedNodeMap nnm = aNode.getAttributes();
                    String name = (nnm.getNamedItem("name")).getNodeValue().trim();
                    String started = parseDate((nnm.getNamedItem("started-at")).getNodeValue().trim());
                    String ended = parseDate((nnm.getNamedItem("finished-at")).getNodeValue().trim());
                    String duration = (nnm.getNamedItem("duration-ms")).getNodeValue().trim();
                    String status = (nnm.getNamedItem("status")).getNodeValue().trim().toLowerCase();
                    String description = "";
                    try {
                        description = (nnm.getNamedItem("description")).getNodeValue().trim().toLowerCase();
                    }
                    catch (Exception e) {
                        // do nothing...there might not be a description
                    }
                    buf.append(className + ", " + name + ", " + status + ", " + duration + ", " + started + ", " + ended + "," + description);
                    parseNode(aNode);
                    // we should now have all the data for our test-method and can prepare it for logging...
                    buf.append("," + parameterString + "," + exceptionName + "," + exceptionMessage + "," + reporterTextString);
                    getLog().info("Parsing results: " + buf.toString());
                    theOutput.add(buf.toString());
                    buf = new StringBuffer();
                }
                else if (aName.equalsIgnoreCase("class")) {
                    NamedNodeMap nnm = aNode.getAttributes();
                    className = (nnm.getNamedItem("name")).getNodeValue().trim();
                }
                else if (aName.equalsIgnoreCase("exception")) {
                    NamedNodeMap nnm = aNode.getAttributes();
                    exceptionName = (nnm.getNamedItem("class")).getNodeValue().trim();
                    // now we want to get the message that goes with the exception
                    Node message = parseNodeNamed("message", aNode);
                    Node cdata = parseNodeNamed("#cdata-section", message);
                    exceptionMessage = cdata.getNodeValue();
                    // since we are formatting for CSV, replace commans with semi-colons
                    exceptionMessage = exceptionMessage.replace(',',';');
                }
                else if (aName.equalsIgnoreCase("params")) {
                    StringBuffer theParams = new StringBuffer();
                    NodeList paramsNodes = aNode.getChildNodes();
                    for (int x = 0; x < paramsNodes.getLength(); ++x) {
                        Node paramsNode = paramsNodes.item(x);
                        if (paramsNode.getNodeName().equalsIgnoreCase("param")) {
                            Node value = parseNodeNamed("value", paramsNode);
                            Node cdata = parseNodeNamed("#cdata-section", value);
                            theParams.append(cdata.getNodeValue() + ",");
                        }
                    }
                    parameterString = theParams.toString();
                    parameterString = parameterString.substring(0, parameterString.length() -1);
                    // since we are formatting for CSV, replace commans with semi-colons
                    parameterString = parameterString.replace(',',';');
                }
                else if (aName.equalsIgnoreCase("reporter-output")) {
                    NodeList lineNodes = aNode.getChildNodes();
                    StringBuffer reporterText = new StringBuffer();
                    for (int x = 0; x < lineNodes.getLength(); ++x) {
                        Node lineNode = lineNodes.item(x);
                        if (lineNode.getNodeName().equalsIgnoreCase("line")) {
                            String lineText = (parseNodeNamed("#cdata-section",lineNodes.item(x))).getNodeValue();
                            reporterText.append(lineText);
                        }
                    }
                    reporterTextString = reporterText.toString();
                    reporterTextString = reporterTextString.replace(',',';');
                }
                parseNode(aNode);
            }
        }
    }
    
    private Node parseNodeNamed (String s, Node n) {
        
        NodeList nl = n.getChildNodes();
        for (int i = 0; i < nl.getLength(); ++i) {
            Node aNode = nl.item(i);
            if (aNode.getNodeName().equalsIgnoreCase(s)) {
                return aNode;
            }
        }
        return null;
    }
    
    private String parseDate (String s) {
        // TestNG format: 2013-07-23T09:19:35Z
        Integer t = s.indexOf("T");
        String part1 = s.substring(0,t);
        String part2 = s.substring(t+1,s.length()-1);
        return part1 + " " + part2;
    }
    
    private String readFile () throws Exception {
        
        StringBuffer buf = new StringBuffer();
        getLog().info("Reading TestNG results file: " + fileLocation + fs + inputFile);
        BufferedReader in = new BufferedReader(new FileReader(fileLocation + fs + inputFile));
        String aLine;
        while ((aLine = in.readLine()) != null) {
            buf.append(aLine);
        }
        return buf.toString();
    }
    
    private void writeCSVFile () throws Exception {
        
        BufferedWriter out = new BufferedWriter(new FileWriter(fileLocation + fs + outputFile));
        // write headers so Splunk knows what to index
        getLog().info("Writing CSV headers: class,method,status,duration, start, end,description, parameters,exception,exception-message,reporter-output");
        out.write("class,method,status,duration, start, end,description,parameters,exception,exception-message,reporter-output");
        out.newLine();
        for (String line : theOutput) {
            getLog().info("Adding to CSV output: " + line);
            out.write(line);
            out.newLine();
        }
        out.flush();
        out.close();
    }
    
    public void execute () throws MojoExecutionException {
        
        try {
            theOutput = new ArrayList<String>();
            String fileText = readFile();
            parseXmlResponse(fileText);
            writeCSVFile();
            getLog().info("CSV file generated: " + fileLocation + fs + outputFile);
        }
        catch (Exception e) {
            getLog().info("Exception occured: " + e.getMessage());
        }
    }
}