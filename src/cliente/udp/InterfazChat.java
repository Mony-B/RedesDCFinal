package cliente.udp; // Se mantiene en el paquete udp porque ahí lo tienes creado

import cliente.tcp.ClienteEnviaTCP2; // Importamos tu clase de transferencia TCP
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class InterfazChat extends JFrame {

    private JTextArea areaChat;
    private JTextField campoMensaje;
    private JButton botonEnviar;
    private JButton botonAdjuntar;
    private JLabel etiquetaArchivo;
    private File archivoSeleccionado;

    // --- CONFIGURACIÓN DE RED ---
    // Cambia aquí la IP y el puerto de tu servidor si es necesario
    private final String SERVER_IP = "localhost"; 
    private final int SERVER_PUERTO = 5000;       

    private DatagramSocket socketUDP;
    private ClienteEscuchaUDP2 hiloEscucha;

    public InterfazChat() {
        // 1. CONFIGURACIÓN DE LA VENTANA PRINCIPAL
        setTitle("Sala de Chat - Redes (UDP + TCP)");
        setSize(500, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Intenta aplicar el estilo visual nativo del sistema operativo
        try { 
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); 
        } catch(Exception e){
            System.err.println("No se pudo establecer el Look and Feel: " + e.getMessage());
        }

        // 2. COMPONENTE: HISTORIAL DE CHAT (Centro)
        areaChat = new JTextArea();
        areaChat.setEditable(false);
        areaChat.setLineWrap(true);
        areaChat.setWrapStyleWord(true); // Evita romper palabras a la mitad
        areaChat.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JScrollPane scrollChat = new JScrollPane(areaChat);
        scrollChat.setBorder(BorderFactory.createTitledBorder("Historial del Chat"));
        add(scrollChat, BorderLayout.CENTER);

        // 3. COMPONENTE: CONTROLES INFERIORES
        JPanel panelInferior = new JPanel(new BorderLayout(5, 5));
        panelInferior.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        // Subpanel superior: Gestión de archivos adjuntos
        JPanel panelArchivo = new JPanel(new BorderLayout(5, 5));
        botonAdjuntar = new JButton("📎 Adjuntar Archivo");
        etiquetaArchivo = new JLabel("Ningún archivo seleccionado.");
        etiquetaArchivo.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        panelArchivo.add(botonAdjuntar, BorderLayout.WEST);
        panelArchivo.add(etiquetaArchivo, BorderLayout.CENTER);

        // Subpanel inferior: Barra de texto y botón enviar
        JPanel panelEscribir = new JPanel(new BorderLayout(5, 5));
        campoMensaje = new JTextField();
        campoMensaje.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        botonEnviar = new JButton("Enviar ➔");
        botonEnviar.setBackground(new Color(34, 139, 34));
        botonEnviar.setForeground(Color.WHITE);
        
        panelEscribir.add(campoMensaje, BorderLayout.CENTER);
        panelEscribir.add(botonEnviar, BorderLayout.EAST);

        // Agrupar ambos subpaneles en el contenedor inferior
        panelInferior.add(panelArchivo, BorderLayout.NORTH);
        panelInferior.add(panelEscribir, BorderLayout.SOUTH);
        add(panelInferior, BorderLayout.SOUTH);

        // 4. INICIALIZAR ARRANQUE DE RED (UDP)
        inicializarConexionRed();

        // 5. EVENTOS Y ACCIONES DE LA INTERFAZ
        
        // Clic en el botón "Enviar"
        botonEnviar.addActionListener(e -> ejecutarEnvio());

        // Presionar ENTER en el teclado dentro de la barra de texto
        campoMensaje.addActionListener(e -> ejecutarEnvio());

        // Clic en el botón "Adjuntar Archivo"
        botonAdjuntar.addActionListener(e -> {
            JFileChooser selector = new JFileChooser();
            int resultado = selector.showOpenDialog(InterfazChat.this);
            if (resultado == JFileChooser.APPROVE_OPTION) {
                archivoSeleccionado = selector.getSelectedFile();
                etiquetaArchivo.setText("Listo: " + archivoSeleccionado.getName());
                areaChat.append("[Local]: Archivo cargado a la cola (" + archivoSeleccionado.getName() + ")\n");
                scrollAbajo();
            }
        });

        // Evento que apaga y libera los sockets si cierras la ventana con la 'X'
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cerrarConexiones();
            }
        });
    }

    private void inicializarConexionRed() {
        try {
            // Inicializa el socket en un puerto aleatorio disponible del cliente
            socketUDP = new DatagramSocket();
            
            // Arranca tu hilo de escucha pasándole este socket y el área de texto
            hiloEscucha = new ClienteEscuchaUDP2(socketUDP, this.areaChat);
            hiloEscucha.start();
            
            areaChat.append("[Sistema]: Chat activo. Destino -> " + SERVER_IP + ":" + SERVER_PUERTO + "\n");
            
        } catch (Exception e) {
            areaChat.append("[Error Red]: No se pudo inicializar los sockets: " + e.getMessage() + "\n");
        }
    }

    private void ejecutarEnvio() {
        String texto = campoMensaje.getText().trim();

        // 1. PROCESAR ENVÍO DE TEXTO NORMAL VÍA UDP
        if (!texto.isEmpty()) {
            try {
                byte[] buffer = texto.getBytes(StandardCharsets.UTF_8);
                InetAddress ipServidor = InetAddress.getByName(SERVER_IP);
                
                DatagramPacket paquete = new DatagramPacket(buffer, buffer.length, ipServidor, SERVER_PUERTO);
                socketUDP.send(paquete);
                
                areaChat.append("Tú: " + texto + "\n");
                campoMensaje.setText(""); 
                scrollAbajo();

                if (texto.equalsIgnoreCase("fin")) {
                    areaChat.append("[Sistema]: Solicitando desconexión...\n");
                    cerrarConexiones();
                    this.dispose();
                    System.exit(0);
                }
            } catch (Exception e) {
                areaChat.append("[Error UDP]: " + e.getMessage() + "\n");
                scrollAbajo();
            }
        }

        // 2. PROCESAR ENVÍO DE ARCHIVO USANDO TU CLASE CLIENTEENVIATCP2
        if (archivoSeleccionado != null) {
            try {
                areaChat.append("[Local]: Abriendo flujo TCP para enviar \"" + archivoSeleccionado.getName() + "\"...\n");
                
                // Instanciamos tu clase mandándole de golpe los 4 parámetros que requiere
                ClienteEnviaTCP2 hiloArchivo = new ClienteEnviaTCP2(
                    SERVER_IP, 
                    SERVER_PUERTO, 
                    archivoSeleccionado, 
                    this.areaChat
                );
                
                // Despega el hilo en segundo plano para no congelar la ventana del chat
                hiloArchivo.start();

            } catch (Exception e) {
                areaChat.append("[Error Canal TCP]: " + e.getMessage() + "\n");
            }

            // Liberar la variable y limpiar el indicador para el siguiente envío
            archivoSeleccionado = null;
            etiquetaArchivo.setText("Ningún archivo seleccionado.");
            scrollAbajo();
        }
    }

    // Fuerza a la barra de desplazamiento a ir al final de forma fluida
    private void scrollAbajo() {
        areaChat.setCaretPosition(areaChat.getDocument().getLength());
    }

    // Método de apagado limpio
    private void cerrarConexiones() {
        if (hiloEscucha != null) {
            hiloEscucha.detener();
        }
        if (socketUDP != null && !socketUDP.isClosed()) {
            socketUDP.close();
        }
    }

    public static void main(String[] args) {
        // Ejecución segura sobre el Event Dispatch Thread de Swing
        SwingUtilities.invokeLater(() -> new InterfazChat().setVisible(true));
    }
}