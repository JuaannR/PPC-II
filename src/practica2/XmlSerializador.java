package practica2;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XmlSerializador {

    private static final String XSD_DISTRIBUCION = "xsd/distribucion.xsd";
    private static final String XSD_CONTROL = "xsd/control.xsd";
    private static final String XSD_RESPUESTA = "xsd/respuesta.xsd";

    private static final Map<String, Schema> esquemas = new ConcurrentHashMap<>();

    private static Schema getEsquema(String ruta) throws Exception {
        Schema s = esquemas.get(ruta);
        if (s == null) {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            s = sf.newSchema(new File(ruta));
            esquemas.put(ruta, s);
        }
        return s;
    }

    // ---------------------------------------------------------------- utilidades DOM

    private static Document nuevoDocumento() throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    }

    private static String aTexto(Document doc) throws Exception {
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StringWriter sw = new StringWriter();
        tf.transform(new DOMSource(doc), new StreamResult(sw));
        return sw.toString();
    }

    private static void anadirElemento(Document doc, Element padre, String nombre, String texto) {
        Element e = doc.createElement(nombre);
        e.appendChild(doc.createTextNode(texto));
        padre.appendChild(e);
    }


    private static Element validarYParsear(String xml, String rutaXsd) throws Exception {
        Validator validador = getEsquema(rutaXsd).newValidator();
        // el XML llega por la red: no se permite cargar DTD ni entidades externas
        validador.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        validador.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        validador.validate(new StreamSource(new StringReader(xml)));

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        return doc.getDocumentElement();
    }

    private static String texto(Element padre, String nombre) {
        return padre.getElementsByTagName(nombre).item(0).getTextContent().trim();
    }

    // ---------------------------------------------------------------- distribucion

    public static String serializar(MensajeDistribucion m) {
        try {
            Document doc = nuevoDocumento();
            Element raiz = doc.createElement("distribucion");
            doc.appendChild(raiz);

            anadirElemento(doc, raiz, "idServidor", String.valueOf(m.getIdServidor()));
            anadirElemento(doc, raiz, "timestamp", String.valueOf(m.getTimestamp()));

            Element lista = doc.createElement("mediciones");
            raiz.appendChild(lista);
            for (Medicion med : m.getMediciones()) {
                Element e = doc.createElement("medicion");
                e.setAttribute("nombre", med.getNombre());
                e.setAttribute("unidad", med.getUnidad());
                e.appendChild(doc.createTextNode(String.valueOf(med.getValor())));
                lista.appendChild(e);
            }
            return aTexto(doc);
        } catch (Exception e) {
            throw new IllegalStateException("Error generando el XML: " + e.getMessage(), e);
        }
    }

    public static MensajeDistribucion deserializar(String xml) throws Exception {
        Element raiz = validarYParsear(xml, XSD_DISTRIBUCION);

        int id = Integer.parseInt(texto(raiz, "idServidor"));
        long ts = Long.parseLong(texto(raiz, "timestamp"));
        MensajeDistribucion m = new MensajeDistribucion(id, ts);

        NodeList nodos = raiz.getElementsByTagName("medicion");
        for (int i = 0; i < nodos.getLength(); i++) {
            Element e = (Element) nodos.item(i);
            m.addMedicion(new Medicion(e.getAttribute("nombre"), e.getAttribute("unidad"),
                    Double.parseDouble(e.getTextContent().trim())));
        }
        return m;
    }

    // ---------------------------------------------------------------- control

    public static String serializar(MensajeControl c) {
        try {
            Document doc = nuevoDocumento();
            Element raiz = doc.createElement("control");
            doc.appendChild(raiz);
            anadirElemento(doc, raiz, "idServidor", String.valueOf(c.getIdServidor()));
            anadirElemento(doc, raiz, "operacion", c.getOperacion() == null ? "" : c.getOperacion());
            anadirElemento(doc, raiz, "parametro", c.getParametro() == null ? "" : c.getParametro());
            return aTexto(doc);
        } catch (Exception e) {
            throw new IllegalStateException("Error generando el XML: " + e.getMessage(), e);
        }
    }

    public static MensajeControl deserializarControl(String xml) throws Exception {
        Element raiz = validarYParsear(xml, XSD_CONTROL);
        return new MensajeControl(Integer.parseInt(texto(raiz, "idServidor")),
                texto(raiz, "operacion"), texto(raiz, "parametro"));
    }

    // ---------------------------------------------------------------- respuesta

    public static String serializar(RespuestaControl r) {
        try {
            Document doc = nuevoDocumento();
            Element raiz = doc.createElement("respuesta");
            doc.appendChild(raiz);
            anadirElemento(doc, raiz, "idServidor", String.valueOf(r.getIdServidor()));
            anadirElemento(doc, raiz, "ok", String.valueOf(r.isOk()));
            anadirElemento(doc, raiz, "detalle", r.getDetalle() == null ? "" : r.getDetalle());
            return aTexto(doc);
        } catch (Exception e) {
            throw new IllegalStateException("Error generando el XML: " + e.getMessage(), e);
        }
    }

    public static RespuestaControl deserializarRespuesta(String xml) throws Exception {
        Element raiz = validarYParsear(xml, XSD_RESPUESTA);
        return new RespuestaControl(Integer.parseInt(texto(raiz, "idServidor")),
                Boolean.parseBoolean(texto(raiz, "ok")), texto(raiz, "detalle"));
    }
}