package practica2;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;

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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XmlSerializador {

    // Ruta relativa a la carpeta desde la que se ejecuta (la raiz del proyecto)
    private static final String RUTA_XSD = "xsd/distribucion.xsd";

    private static Schema esquema; // se carga la primera vez que se necesita

    private static synchronized Schema getEsquema() throws Exception {
        if (esquema == null) {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            esquema = sf.newSchema(new File(RUTA_XSD));
        }
        return esquema;
    }

    // ------------------------------------------------------------------ objeto -> XML (DOM)

    public static String serializar(MensajeDistribucion m) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

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

            Transformer tf = TransformerFactory.newInstance().newTransformer();
            tf.setOutputProperty(OutputKeys.INDENT, "yes");
            tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StringWriter sw = new StringWriter();
            tf.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Error generando el XML: " + e.getMessage(), e);
        }
    }

    private static void anadirElemento(Document doc, Element padre, String nombre, String texto) {
        Element e = doc.createElement(nombre);
        e.appendChild(doc.createTextNode(texto));
        padre.appendChild(e);
    }

    // ------------------------------------------------------------------ XML -> objeto (validando)

    public static MensajeDistribucion deserializar(String xml) throws Exception {
        // 1) Validacion explicita contra el XSD: si no es valido, lanza SAXException
        getEsquema().newValidator().validate(new StreamSource(new StringReader(xml)));

        // 2) Ya es valido: construimos el arbol DOM y lo recorremos
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));
        Element raiz = doc.getDocumentElement();

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

    private static String texto(Element padre, String nombre) {
        return padre.getElementsByTagName(nombre).item(0).getTextContent().trim();
    }
}