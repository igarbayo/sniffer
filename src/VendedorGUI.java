import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class VendedorGUI extends JFrame {
    private JTable subastaTable; // La tabla para mostrar las subastas activas
    private DefaultTableModel tableModel; // El modelo de la tabla
    private Vendedor vendedor; // Instancia del vendedor

    // Campos para añadir libros
    private JTextField libroTextField;
    private JTextField precioTextField;
    private JTextField incrementoTextField;
    private JButton iniciarSubastaButton;

    public VendedorGUI(Vendedor vendedor, List<DataVendedor> subastasData) {
        this.vendedor = vendedor;
        setTitle(this.vendedor.getLocalName());
        setSize(600, 400); // Aseguramos que haya espacio para la tabla y los campos
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centrar la ventana en la pantalla

        // Crear los componentes para la lógica de añadir subasta
        libroTextField = new JTextField(20);
        precioTextField = new JTextField(10);
        incrementoTextField = new JTextField(10);
        iniciarSubastaButton = new JButton("Iniciar Subasta");

        // Crear el modelo de la tabla
        String[] columnNames = {"Libro", "Precio", "Ganador", "Número de Respondedores"};
        tableModel = new DefaultTableModel(columnNames, 0);

        // Crear la tabla con el modelo
        subastaTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(subastaTable); // Agregar la tabla a un JScrollPane

        // Llenar la tabla con los datos de las subastas activas
        actualizarTabla(subastasData);

        // Acción del botón "Iniciar Subasta"
        iniciarSubastaButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String libro = libroTextField.getText().trim();
                String precioText = precioTextField.getText().trim();
                String incrementoText = incrementoTextField.getText().trim();

                // Validar los datos de entrada
                if (libro.isEmpty() || precioText.isEmpty() || incrementoText.isEmpty()) {
                    JOptionPane.showMessageDialog(VendedorGUI.this, "Por favor, complete todos los campos.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    int precio = Integer.parseInt(precioText);
                    int incremento = Integer.parseInt(incrementoText);

                    // Llamar al método para iniciar la subasta (debes implementarlo en tu clase Vendedor)
                    vendedor.anadirLibro(libro, precio, incremento);

                    // Actualizar la tabla con las nuevas subastas después de añadir una nueva
                    actualizarTabla(vendedor.obtenerSubastasData());

                    JOptionPane.showMessageDialog(VendedorGUI.this, "Subasta iniciada correctamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);

                    // Limpiar los campos de texto después de añadir la subasta
                    libroTextField.setText("");
                    precioTextField.setText("");
                    incrementoTextField.setText("");
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(VendedorGUI.this, "El precio y el incremento deben ser números válidos.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Crear el layout con BoxLayout para la parte superior
        JPanel panelSuperior = new JPanel();
        panelSuperior.setLayout(new GridLayout(6, 3)); // Apilar los componentes verticalmente
        panelSuperior.setAlignmentX(Component.CENTER_ALIGNMENT); // Alineación a la izquierda

        // Agregar los componentes de los campos de entrada y el botón
        panelSuperior.add(new JLabel(" "));
        panelSuperior.add(new JLabel("      GESTIÓN DE SUBASTAS"));
        panelSuperior.add(new JLabel(" "));
        panelSuperior.add(new JLabel("Libro:"));
        panelSuperior.add(libroTextField);
        panelSuperior.add(new JLabel(" "));
        panelSuperior.add(new JLabel("Precio inicial:"));
        panelSuperior.add(precioTextField);
        panelSuperior.add(new JLabel(" "));
        panelSuperior.add(new JLabel("Incremento:"));
        panelSuperior.add(incrementoTextField);
        panelSuperior.add(iniciarSubastaButton);
        panelSuperior.add(new JLabel(" "));
        panelSuperior.add(new JLabel(" "));
        panelSuperior.add(new JLabel(" "));
        panelSuperior.add(new JLabel(" "));
        panelSuperior.add(new JLabel("         SUBASTAS ACTIVAS"));
        panelSuperior.add(new JLabel(" "));

        // Agregar la tabla
        add(panelSuperior, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        setVisible(true);
    }

    public void actualizarTabla(List<DataVendedor> subastasData) {
        // Limpiar la tabla antes de agregar nuevos datos
        tableModel.setRowCount(0);

        // Iterar sobre la lista de subastas y agregar una fila por cada subasta
        for (DataVendedor data : subastasData) {
            String libro = data.getLibro();
            int precio = data.getPrecio();
            String ganador = data.getGanador();
            int numRespondedores = data.getNumRespondedores();

            // Agregar la fila a la tabla
            tableModel.addRow(new Object[]{libro, precio, ganador, numRespondedores});
        }
    }
}
