import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

public class CompradorGUI extends JFrame {
    private JTextField libroTextField; // Campo de texto para el nombre del libro
    private JTextField maximoTextField; // Campo de texto para el precio máximo
    private JButton anadirButton; // Botón para añadir libro
    private JButton abandonarButton; // Botón para abandonar subasta
    private JTextField libroAbandonarTextField; // Campo de texto para el libro a abandonar
    private JTable tablaSubastas; // Tabla para mostrar las subastas
    private DefaultTableModel tableModel; // Modelo para la tabla de subastas
    private JTable tablaLibrosDeseados; // Tabla para mostrar los libros deseados
    private JTable tablaLibrosComprados;    // Tabla para mostar los libros comprados
    private DefaultTableModel librosDeseadosModel; // Modelo para la tabla de libros deseados
    private DefaultTableModel librosCompradosModel; // Modelo para la tabla de libros comprados
    private JButton eliminarButton; // Botón para eliminar libro
    private JTextField libroEliminarTextField; // Campo de texto para el libro a eliminar

    private Comprador comprador; // Instancia del comprador

    public CompradorGUI(Comprador comprador) {
        this.comprador = comprador;
        setTitle(this.comprador.getLocalName());
        setSize(600, 500); // Ajustamos el tamaño para acomodar ambas tablas
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centrar la ventana en la pantalla

        // Crear los componentes
        libroTextField = new JTextField(20);
        maximoTextField = new JTextField(10);
        anadirButton = new JButton("Añadir libro");
        abandonarButton = new JButton("Abandonar subasta");
        libroAbandonarTextField = new JTextField(20);

        // Componentes para eliminar libros
        eliminarButton = new JButton("Eliminar libro");
        libroEliminarTextField = new JTextField(20);

        // Crear el modelo de la tabla de subastas
        tableModel = new DefaultTableModel();
        tableModel.addColumn("Libro");
        tableModel.addColumn("Precio Ronda");
        tableModel.addColumn("Soy Ganador Actual");

        // Crear la tabla de subastas
        tablaSubastas = new JTable(tableModel);
        JScrollPane scrollPaneTablaSubastas = new JScrollPane(tablaSubastas);
        JPanel panelSubastas = new JPanel();
        panelSubastas.setLayout(new GridLayout(1, 3));
        panelSubastas.add(new JLabel(" "));
        panelSubastas.add(new JLabel("          SUBASTAS ACTIVAS"));
        panelSubastas.add(new JLabel(" "));
        JPanel panelSubastas2 = new JPanel();
        panelSubastas2.setLayout(new BoxLayout(panelSubastas2, BoxLayout.Y_AXIS));
        panelSubastas2.add(panelSubastas);
        panelSubastas2.add(scrollPaneTablaSubastas);

        // Crear el modelo de la tabla de libros deseados
        librosDeseadosModel = new DefaultTableModel();
        librosDeseadosModel.addColumn("Libro");
        librosDeseadosModel.addColumn("Precio Máximo");

        // Crear el modelo de la tabla de libros comprados
        librosCompradosModel = new DefaultTableModel();
        librosCompradosModel.addColumn("Libro");
        librosCompradosModel.addColumn("Precio");

        // Crear la tabla de libros deseados
        tablaLibrosDeseados = new JTable(librosDeseadosModel);
        JScrollPane scrollPaneTablaLibrosDeseados = new JScrollPane(tablaLibrosDeseados);
        JPanel panelLibrosDeseados = new JPanel();
        panelLibrosDeseados.setLayout(new GridLayout(1, 3));
        panelLibrosDeseados.add(new JLabel(" "));
        panelLibrosDeseados.add(new JLabel("           LIBROS DESEADOS"));
        panelLibrosDeseados.add(new JLabel(" "));
        JPanel panelLibrosDeseados2 = new JPanel();
        panelLibrosDeseados2.setLayout(new BoxLayout(panelLibrosDeseados2, BoxLayout.Y_AXIS));
        panelLibrosDeseados2.add(panelLibrosDeseados);
        panelLibrosDeseados2.add(scrollPaneTablaLibrosDeseados);

        // Crear la tabla de libros comprados
        tablaLibrosComprados = new JTable(librosCompradosModel);
        JScrollPane scrollPaneTablaLibrosComprados = new JScrollPane(tablaLibrosComprados);
        JPanel panelLibrosComprados = new JPanel();
        panelLibrosComprados.setLayout(new GridLayout(1, 3));
        panelLibrosComprados.add(new JLabel(" "));
        panelLibrosComprados.add(new JLabel("         LIBROS COMPRADOS"));
        panelLibrosComprados.add(new JLabel(" "));
        JPanel panelLibrosComprados2 = new JPanel();
        panelLibrosComprados2.setLayout(new BoxLayout(panelLibrosComprados2, BoxLayout.Y_AXIS));
        panelLibrosComprados2.add(panelLibrosComprados);
        panelLibrosComprados2.add(scrollPaneTablaLibrosComprados);

        // Crear el layout
        setLayout(new BorderLayout()); // Usamos BorderLayout para dividir el espacio

        JPanel panelFormulario = new JPanel();
        panelFormulario.setLayout(new GridLayout(8, 3)); // Usamos GridLayout para los campos y botones
        panelFormulario.add(new JLabel(" "));
        panelFormulario.add(new JLabel("         GESTIÓN DE LIBROS"));
        panelFormulario.add(new JLabel(" "));
        panelFormulario.add(new JLabel("Libro nuevo:"));
        panelFormulario.add(libroTextField);
        panelFormulario.add(new JLabel(" "));
        panelFormulario.add(new JLabel("Precio máximo:"));
        panelFormulario.add(maximoTextField);
        panelFormulario.add(anadirButton);
        panelFormulario.add(new JLabel(" "));
        panelFormulario.add(new JLabel(" "));
        panelFormulario.add(new JLabel(" "));
        panelFormulario.add(new JLabel("Libro a abandonar:"));
        panelFormulario.add(libroAbandonarTextField);
        panelFormulario.add(abandonarButton);
        panelFormulario.add(new JLabel(" "));
        panelFormulario.add(new JLabel(" "));
        panelFormulario.add(new JLabel(" "));
        panelFormulario.add(new JLabel("Libro a eliminar:"));
        panelFormulario.add(libroEliminarTextField);
        panelFormulario.add(eliminarButton); // Añadir el botón eliminar
        panelFormulario.add(new JLabel(" "));
        panelFormulario.add(new JLabel(" "));
        panelFormulario.add(new JLabel(" "));

        // Panel para las tablas
        JPanel panelTablas = new JPanel();
        panelTablas.setLayout(new GridLayout(3, 1)); // Usamos GridLayout para apilar las tablas
        panelTablas.add(panelLibrosDeseados2);
        panelTablas.add(panelSubastas2);
        panelTablas.add(panelLibrosComprados2);

        add(panelFormulario, BorderLayout.NORTH); // Agregamos el formulario arriba de la ventana
        add(panelTablas, BorderLayout.CENTER); // Agregamos las tablas al centro

        // Acción del botón "Añadir Libro"
        anadirButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String libro = libroTextField.getText().trim();
                String maximoText = maximoTextField.getText().trim();

                // Validar los datos de entrada
                if (libro.isEmpty() || maximoText.isEmpty()) {
                    JOptionPane.showMessageDialog(CompradorGUI.this, "Por favor, complete todos los campos.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    int maximo = Integer.parseInt(maximoText);
                    comprador.anadirLibro(libro, maximo); // Llamar al método anadirLibro del comprador
                    actualizarTablaLibrosDeseados(comprador.getLibrosDeseados());
                    JOptionPane.showMessageDialog(CompradorGUI.this, "Libro añadido correctamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    libroTextField.setText(""); // Limpiar el campo libro
                    maximoTextField.setText(""); // Limpiar el campo precio máximo
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(CompradorGUI.this, "El precio máximo debe ser un número válido.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Acción del botón "Eliminar Libro"
        eliminarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String libro = libroEliminarTextField.getText().trim();

                // Validar que se ha ingresado un libro
                if (libro.isEmpty()) {
                    JOptionPane.showMessageDialog(CompradorGUI.this, "Por favor, ingrese el nombre del libro a eliminar.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Llamar al método eliminarLibro del comprador
                boolean resultado = comprador.eliminarLibro(libro);

                if (resultado) {
                    actualizarTablaLibrosDeseados(comprador.getLibrosDeseados());
                    JOptionPane.showMessageDialog(CompradorGUI.this, "Libro eliminado correctamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    libroEliminarTextField.setText(""); // Limpiar el campo libro a eliminar
                } else {
                    JOptionPane.showMessageDialog(CompradorGUI.this, "El libro no se encuentra en la lista de libros deseados.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        setVisible(true);

        // Acción del botón "Abandonar Subasta"
        abandonarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String libro = libroAbandonarTextField.getText().trim();

                // Validar que se ha ingresado un libro
                if (libro.isEmpty()) {
                    JOptionPane.showMessageDialog(CompradorGUI.this, "Por favor, ingrese el nombre del libro a abandonar.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Llamar al método abandonarSubasta del comprador
                boolean resultado = comprador.abandonarSubasta(libro);

                if (resultado) {
                    actualizarTablaLibrosDeseados(comprador.getLibrosDeseados());
                    JOptionPane.showMessageDialog(CompradorGUI.this, "Subasta abandonada correctamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    libroAbandonarTextField.setText(""); // Limpiar el campo libro a abandonar
                } else {
                    JOptionPane.showMessageDialog(CompradorGUI.this, "No se puede abandonar la subasta en este momento.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        setVisible(true);
    }

    public void actualizarTabla(List<DataComprador> compradoresData) {
        // Limpiar la tabla de subastas antes de agregar nuevos datos
        tableModel.setRowCount(0);

        // Iterar sobre la lista de compradores y agregar una fila por cada comprador
        for (DataComprador data : compradoresData) {
            String libro = data.getLibro();
            int precioRonda = data.getPrecioRonda();
            // Convertir el booleano soyGanadorActual a un valor legible (Sí/No)
            String soyGanador = data.isSoyGanadorActual() ? "Sí" : "No";

            // Agregar la fila a la tabla
            tableModel.addRow(new Object[]{libro, precioRonda, soyGanador});
        }
    }

    public void actualizarTablaLibrosDeseados(Map<String, Integer> librosDeseados) {
        // Limpiar la tabla de libros deseados antes de agregar nuevos datos
        librosDeseadosModel.setRowCount(0);

        // Iterar sobre el mapa y agregar una fila por cada entrada
        for (Map.Entry<String, Integer> entry : librosDeseados.entrySet()) {
            String libro = entry.getKey();
            int precioMaximo = entry.getValue();
            librosDeseadosModel.addRow(new Object[]{libro, precioMaximo});
        }
    }

    public void actualizarTablaComprados(Map<String, Integer> comprados) {
        librosCompradosModel.setRowCount(0);
        for (Map.Entry<String, Integer> entry : comprados.entrySet()) {
            String libro = entry.getKey();
            int precio = entry.getValue();
            librosCompradosModel.addRow(new Object[]{libro, precio});
        }
    }

}
