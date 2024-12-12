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
    private DefaultTableModel librosDeseadosModel; // Modelo para la tabla de libros deseados
    private JButton eliminarButton; // Botón para eliminar libro
    private JTextField libroEliminarTextField; // Campo de texto para el libro a eliminar

    private Comprador comprador; // Instancia del comprador

    public CompradorGUI(Comprador comprador) {
        this.comprador = comprador;
        setTitle("Interfaz Comprador");
        setSize(600, 600); // Ajustamos el tamaño para acomodar ambas tablas
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
        panelSubastas.add(new JLabel("        LISTA DE SUBASTAS"));
        panelSubastas.add(new JLabel(" "));
        JPanel panelSubastas2 = new JPanel();
        panelSubastas2.setLayout(new GridLayout(2, 1));
        panelSubastas2.add(panelSubastas);
        panelSubastas2.add(scrollPaneTablaSubastas);

        // Crear el modelo de la tabla de libros deseados
        librosDeseadosModel = new DefaultTableModel();
        librosDeseadosModel.addColumn("Libro");
        librosDeseadosModel.addColumn("Precio Máximo");

        // Crear la tabla de libros deseados
        tablaLibrosDeseados = new JTable(librosDeseadosModel);
        JScrollPane scrollPaneTablaLibrosDeseados = new JScrollPane(tablaLibrosDeseados);

        // Crear el layout
        setLayout(new BorderLayout()); // Usamos BorderLayout para dividir el espacio

        JPanel panelFormulario = new JPanel();
        panelFormulario.setLayout(new GridLayout(9, 3)); // Usamos GridLayout para los campos y botones
        panelFormulario.add(new JLabel(" "));
        panelFormulario.add(new JLabel("        GESTIÓN DE LIBROS"));
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
        panelFormulario.add(new JLabel(" "));
        panelFormulario.add(new JLabel("          LISTA DE LIBROS"));
        panelFormulario.add(new JLabel(" "));

        // Panel para las tablas
        JPanel panelTablas = new JPanel();
        panelTablas.setLayout(new GridLayout(2, 1)); // Usamos GridLayout para apilar las tablas
        panelTablas.add(scrollPaneTablaLibrosDeseados);
        panelTablas.add(panelSubastas2);

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
}
