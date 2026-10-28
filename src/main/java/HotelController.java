import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HotelController {

    // ---- Modelos ----
    public record Habitacion(String numero, String estado, String planta, String tipo,
                             String capacidad, String precio, String vista, String accesible) {
        boolean contiene(String t) {
            return String.join(" ", numero, estado, planta, tipo, capacidad, precio, vista, accesible)
                    .toLowerCase().contains(t);
        }
    }

    public record Reserva(String codigo, String cliente, String email, String habitacion,
                          String entrada, String salida, String personas, String estado) {
        boolean contiene(String t) {
            return String.join(" ", codigo, cliente, email, habitacion, entrada, salida, personas, estado)
                    .toLowerCase().contains(t);
        }
    }

    // ---- Cabecera ----
    @FXML private Label lblNombre;
    @FXML private Label lblCategoria;
    @FXML private Label lblCiudad;

    // ---- Búsqueda / carga ----
    @FXML private TextField txtBuscar;
    @FXML private Button btnAñadirXml;

    // ---- Tabla habitaciones ----
    @FXML private TableView<Habitacion> tablaHab;
    @FXML private TableColumn<Habitacion, String> colNum, colEstadoHab, colPlanta, colTipo,
            colCap, colPrecio, colVista, colAccesible;

    // ---- Tabla reservas ----
    @FXML private TableView<Reserva> tablaRes;
    @FXML private TableColumn<Reserva, String> colCodigo, colCliente, colEmail, colHabRes,
            colEntrada, colSalida, colPersonas, colEstadoRes;

    // ---- Pie ----
    @FXML private Label lblEstado;

    private final ObservableList<Habitacion> habitaciones = FXCollections.observableArrayList();
    private final ObservableList<Reserva> reservas = FXCollections.observableArrayList();
    private FilteredList<Habitacion> habFiltradas;
    private FilteredList<Reserva> resFiltradas;

    @FXML
    private void initialize() {
        colNum.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().numero()));
        colEstadoHab.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().estado()));
        colPlanta.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().planta()));
        colTipo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().tipo()));
        colCap.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().capacidad()));
        colPrecio.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().precio()));
        colVista.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().vista()));
        colAccesible.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().accesible()));

        colCodigo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().codigo()));
        colCliente.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().cliente()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().email()));
        colHabRes.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().habitacion()));
        colEntrada.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().entrada()));
        colSalida.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().salida()));
        colPersonas.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().personas()));
        colEstadoRes.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().estado()));

        habFiltradas = new FilteredList<>(habitaciones, h -> true);
        resFiltradas = new FilteredList<>(reservas, r -> true);
        tablaHab.setItems(habFiltradas);
        tablaRes.setItems(resFiltradas);

        // Filtrado en vivo mientras se escribe
        txtBuscar.textProperty().addListener((obs, o, n) -> buscar());
    }

    // ---- Acciones ----
    @FXML
    private void buscar() {
        String t = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        habFiltradas.setPredicate(h -> t.isEmpty() || h.contiene(t));
        resFiltradas.setPredicate(r -> t.isEmpty() || r.contiene(t));
    }

    @FXML
    private void seleccionarXml() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecciona un archivo XML");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos XML", "*.xml"));
        File dir = new File(System.getProperty("user.dir"));
        if (dir.isDirectory()) fc.setInitialDirectory(dir);

        File f = fc.showOpenDialog(btnAñadirXml.getScene().getWindow());
        if (f == null) return;

        try {
            cargarXml(f);
        } catch (Exception e) {
            lblEstado.setText("Error al leer " + f.getName() + ": " + e.getMessage());
        }
    }

    // ---- Lectura del XML (tolerante: atributos o elementos hijo) ----
    private void cargarXml(File f) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setExpandEntityReferences(false);
        Document doc = dbf.newDocumentBuilder().parse(f);
        doc.getDocumentElement().normalize();

        Element root = doc.getDocumentElement();
        Element hotel = "hotel".equalsIgnoreCase(root.getTagName()) ? root : primero(root, "hotel");
        if (hotel == null) hotel = root;

        String cat = dato(hotel, "categoria", "categoría", "estrellas");
        String ciudad = dato(hotel, "ciudad");
        String dir = dato(hotel, "direccion", "dirección", "calle");
        String tel = dato(hotel, "telefono", "teléfono", "tel");

        if (!cat.isEmpty()) lblCategoria.setText(cat.matches("\\d") ? "★".repeat(Integer.parseInt(cat)) : cat);
        List<String> partes = new ArrayList<>();
        if (!ciudad.isEmpty()) partes.add(ciudad);
        if (!dir.isEmpty()) partes.add(dir);
        if (!tel.isEmpty()) partes.add(tel);
        if (!partes.isEmpty()) lblCiudad.setText(String.join(" - ", partes));

        List<Habitacion> hs = new ArrayList<>();
        for (Element e : elementos(doc, "habitacion", "habitación")) {
            // Las <habitacion> anidadas dentro de <reserva> no son habitaciones del hotel
            if (e.getParentNode() instanceof Element p && p.getTagName().equalsIgnoreCase("reserva")) continue;
            hs.add(new Habitacion(
                    dato(e, "numero", "número", "num", "id"),
                    dato(e, "estado"),
                    dato(e, "planta", "piso"),
                    dato(e, "tipo"),
                    dato(e, "capacidad"),
                    dato(e, "precio", "precioNoche", "precio_noche"),
                    dato(e, "vista"),
                    dato(e, "accesible")));
        }

        List<Reserva> rs = new ArrayList<>();
        for (Element e : elementos(doc, "reserva")) {
            rs.add(new Reserva(
                    dato(e, "codigo", "código", "id"),
                    dato(e, "cliente", "nombre"),
                    dato(e, "email", "correo"),
                    dato(e, "habitacion", "habitación", "numHabitacion", "hab"),
                    dato(e, "entrada", "fechaEntrada", "checkin"),
                    dato(e, "salida", "fechaSalida", "checkout"),
                    dato(e, "personas", "numPersonas"),
                    dato(e, "estado")));
        }

        habitaciones.setAll(hs);
        reservas.setAll(rs);
        buscar();
        lblEstado.setText(f.getName() + ": " + hs.size() + " habitaciones y " + rs.size() + " reservas cargadas.");
    }

    private static List<Element> elementos(Document doc, String... tags) {
        List<Element> out = new ArrayList<>();
        for (String t : tags) {
            NodeList nl = doc.getElementsByTagName(t);
            for (int i = 0; i < nl.getLength(); i++) out.add((Element) nl.item(i));
        }
        return out;
    }

    private static Element primero(Element e, String tag) {
        NodeList nl = e.getElementsByTagName(tag);
        return nl.getLength() > 0 ? (Element) nl.item(0) : null;
    }

    /** Devuelve el primer valor encontrado como atributo o como elemento hijo (o descendiente). */
    private static String dato(Element e, String... nombres) {
        for (String n : nombres) {
            if (e.hasAttribute(n)) return e.getAttribute(n).trim();
        }
        for (String n : nombres) {
            NodeList nl = e.getElementsByTagName(n);
            for (int i = 0; i < nl.getLength(); i++) {
                Element c = (Element) nl.item(i);
                String txt = c.getTextContent() == null ? "" : c.getTextContent().trim();
                if (!txt.isEmpty() && c.getChildNodes().getLength() > 0 && hijosSoloTexto(c)) return txt;
                // elemento vacío con atributo (p. ej. <habitacion numero="101"/>)
                for (String a : new String[]{"numero", "número", "id", "codigo"}) {
                    if (c.hasAttribute(a)) return c.getAttribute(a).trim();
                }
            }
        }
        return "";
    }

    private static boolean hijosSoloTexto(Element c) {
        NodeList h = c.getChildNodes();
        for (int i = 0; i < h.getLength(); i++) {
            if (h.item(i).getNodeType() == Node.ELEMENT_NODE) return false;
        }
        return true;
    }
}
