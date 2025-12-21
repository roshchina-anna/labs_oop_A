package io;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import functions.Point;
import functions.TabulatedFunction;
import functions.factory.TabulatedFunctionFactory;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public final class FunctionsIO {
    private static final Logger logger = LoggerFactory.getLogger(FunctionsIO.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private FunctionsIO() {
        throw new UnsupportedOperationException();
    }

    public static void writeTabulatedFunction(BufferedOutputStream outputStream, TabulatedFunction function) throws IOException {
        logger.debug("Writing tabulated function to binary stream ({} points)", function.getCount());
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        dataOutputStream.writeInt(function.getCount());
        for (Point point : function) {
            dataOutputStream.writeDouble(point.x);
            dataOutputStream.writeDouble(point.y);
        }
        outputStream.flush();
        logger.trace("Successfully wrote {} points to binary stream", function.getCount());
    }


    public static TabulatedFunction readTabulatedFunction(BufferedInputStream inputStream, TabulatedFunctionFactory factory) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        int count = dataInputStream.readInt();
        double[] xValues = new double[count];
        double[] yValues = new double[count];
        for (int i = 0; i < count; i++) {
            xValues[i] = dataInputStream.readDouble();
            yValues[i] = dataInputStream.readDouble();
        }
        return factory.create(xValues, yValues);
    }

    public static TabulatedFunction deserialize(BufferedInputStream stream) throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(stream);
        return (TabulatedFunction) objectInputStream.readObject();
    }

    public static void serialize(BufferedOutputStream stream, TabulatedFunction function) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(stream);
        objectOutputStream.writeObject(function);
        stream.flush();
    }
    public static void writeTabulatedFunction(BufferedWriter writer, TabulatedFunction function) throws IOException {
        PrintWriter printWriter = new PrintWriter(writer);
        printWriter.println(function.getCount());
        for (Point point : function) {
            printWriter.printf("%f %f\n", point.x, point.y);
        }
        printWriter.flush();
    }
    public static TabulatedFunction readTabulatedFunction(BufferedReader reader, TabulatedFunctionFactory factory) throws IOException {
        String countLine = reader.readLine();
        int count = Integer.parseInt(countLine);

        double[] xValues = new double[count];
        double[] yValues = new double[count];
        NumberFormat format = NumberFormat.getInstance(Locale.forLanguageTag("ru"));
        for (int i = 0; i < count; i++) {
            String line = reader.readLine();
            String[] parts = line.split(" ");
            try {
                Number xNumber = format.parse(parts[0]);
                Number yNumber = format.parse(parts[1]);
                xValues[i] = xNumber.doubleValue();
                yValues[i] = yNumber.doubleValue();
            } catch (ParseException e) {
                logger.error("Parse error at line {}: '{}'", i + 2, line, e);
                throw new IOException(e);
            }
        }
        return factory.create(xValues, yValues);
    }
    public static void writeTabulatedFunctionJson(Writer writer, TabulatedFunction function) throws IOException {
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(writer, TabulatedFunctionData.from(function));
        writer.flush();
    }
    public static TabulatedFunction readTabulatedFunctionJson(Reader reader, TabulatedFunctionFactory factory) throws IOException {
        TabulatedFunctionData data = OBJECT_MAPPER.readValue(reader, TabulatedFunctionData.class);
        return factory.create(data.getXValues(), data.getYValues());
    }

    public static void writeTabulatedFunctionXml(Writer writer, TabulatedFunction function) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();

            Element root = document.createElement("tabulatedFunction");
            document.appendChild(root);

            for (Point point : function) {
                Element pointElement = document.createElement("point");

                Element xElement = document.createElement("x");
                xElement.setTextContent(Double.toString(point.x));
                pointElement.appendChild(xElement);

                Element yElement = document.createElement("y");
                yElement.setTextContent(Double.toString(point.y));
                pointElement.appendChild(yElement);

                root.appendChild(pointElement);
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(document), new StreamResult(writer));
        } catch (ParserConfigurationException | TransformerException e) {
            logger.error("Failed to write tabulated function to XML", e);
            throw new IOException("Failed to write XML", e);
        }
    }

    public static TabulatedFunction readTabulatedFunctionXml(Reader reader, TabulatedFunctionFactory factory) throws IOException {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(reader));

            NodeList pointNodes = document.getElementsByTagName("point");
            int count = pointNodes.getLength();
            double[] xValues = new double[count];
            double[] yValues = new double[count];

            for (int i = 0; i < count; i++) {
                Element pointElement = (Element) pointNodes.item(i);
                double x = Double.parseDouble(pointElement.getElementsByTagName("x").item(0).getTextContent());
                double y = Double.parseDouble(pointElement.getElementsByTagName("y").item(0).getTextContent());
                xValues[i] = x;
                yValues[i] = y;
            }
            return factory.create(xValues, yValues);
        } catch (ParserConfigurationException | SAXException e) {
            logger.error("Failed to parse tabulated function from XML", e);
            throw new IOException("Failed to read XML", e);
        }
    }

    private static record TabulatedFunctionData(@JsonProperty("points") List<PointData> points) {
        @JsonCreator
        public TabulatedFunctionData {
        }
        public static TabulatedFunctionData from(TabulatedFunction function) {
            List<PointData> points = new ArrayList<>();
            for (Point point : function) {
                points.add(new PointData(point.x, point.y));
            }
            return new TabulatedFunctionData(points);
        }
        @JsonIgnore
        public double[] getXValues() {
            return points.stream().mapToDouble(PointData::x).toArray();
        }
        @JsonIgnore
        public double[] getYValues() {
            return points.stream().mapToDouble(PointData::y).toArray();
        }
    }
    private static record PointData(@JsonProperty("x") double x, @JsonProperty("y") double y) {
        @JsonCreator
        public PointData {

        }
    }
}