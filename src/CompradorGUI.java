import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class CompradorGUI extends JFrame {
    private JTextField libroTextField; // Campo de texto para el nombre del libro
    private JTextField maximoTextField; // Campo de texto para el precio máximo
    private JButton anadirButton; // Botón para añadir libro
    private JButton abandonarButton; // Botón para abandonar subasta
    private JTextField libroAbandonarTextField; // Campo de texto para el libro a abandonar
    private JTable tablaSubastas; // Tabla para mostrar las subastas
    private DefaultTableModel tableModel; // Modelo para la tabla
    private Comprador comprador; // Instancia del comprador

    public CompradorGUI(Comprador comprador) {
        this.comprador = comprador;
        setTitle("Interfaz Comprador");
        setSize(600, 400); // Aumentamos el tamaño para que quepan más elementos
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centrar la ventana en la pantalla

        // Crear los componentes
        libroTextField = new JTextField(20);
        maximoTextField = new JTextField(10);
        anadirButton = new JButton("Añadir Libro");
        abandonarButton = new JButton("Abandonar Subasta");
        libroAbandonarTextField = new JTextField(20);

        // Crear el modelo de la tabla
        tableModel = new DefaultTableModel();
        tableModel.addColumn("Libro");
        tableModel.addColumn("Precio Ronda");
        tableModel.addColumn("Soy Ganador Actual");

        // Crear la tabla
        tablaSubastas = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(tablaSubastas);

        // Crear el layout
        setLayout(new BorderLayout()); // Usamos BorderLayout para dividir el espacio

        JPanel panelFormulario = new JPanel();
        panelFormulario.setLayout(new GridLayout(4, 2)); // Usamos GridLayout para los campos y botones
        panelFormulario.add(new JLabel("Libro:"));
        panelFormulario.add(libroTextField);
        panelFormulario.add(new JLabel("Precio Máximo:"));
        panelFormulario.add(maximoTextField);
        panelFormulario.add(anadirButton);
        panelFormulario.add(new JLabel("Libro a Abandonar:"));
        panelFormulario.add(libroAbandonarTextField);
        panelFormulario.add(abandonarButton);

        add(panelFormulario, BorderLayout.NORTH); // Agregamos el formulario arriba de la ventana
        add(scrollPane, BorderLayout.CENTER); // Agregamos la tabla al centro

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
                    JOptionPane.showMessageDialog(CompradorGUI.this, "Libro añadido correctamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    libroTextField.setText(""); // Limpiar el campo libro
                    maximoTextField.setText(""); // Limpiar el campo precio máximo
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(CompradorGUI.this, "El precio máximo debe ser un número válido.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

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
        // Limpiar la tabla antes de agregar nuevos datos
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
}
