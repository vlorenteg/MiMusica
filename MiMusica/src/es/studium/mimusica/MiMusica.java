package es.studium.mimusica;

import javazoom.jl.player.Player;


import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class MiMusica extends JFrame {

    private static final long serialVersionUID = 1L;
    private DefaultListModel<String> listaModelo; // Modelo de la lista
    private JList<String> cancionesLista; // Lista de canciones
    private JButton reproducirBoton, pararBoton;
    private List<MiMusicaThread> activeThreads; // Lista de hilos activos

    public MiMusica() {
        // Configuración básica de la ventana
        setTitle("Mi Música");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Inicializa la lista y su modelo
        listaModelo = new DefaultListModel<>();
        cancionesLista = new JList<>(listaModelo);
        JScrollPane scrollPane = new JScrollPane(cancionesLista);

        // Inicializa botones
        reproducirBoton = new JButton("Reproducir");
        pararBoton = new JButton("Parar");

        // Panel para botones
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(reproducirBoton);
        buttonPanel.add(pararBoton);

        // Agrega componentes al JFrame
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Listeners de botones
        reproducirBoton.addActionListener(e -> reproducirMusica());
        pararBoton.addActionListener(e -> pararMusica());
        
        cancionesLista.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {  // Verifica si es un doble clic
                    reproducirMusica();
                }
            }
        });

        // Lista de hilos activos
        activeThreads = new ArrayList<>();

        // Busca archivos de música
        buscarMusica();

        setVisible(true);
    }

    private void buscarMusica() {
        // Define las extensiones como una ArrayList
        ArrayList<String> extensions = new ArrayList<>();
        extensions.add("mp3");
        extensions.add("wav");

        File[] roots = File.listRoots(); // Obtiene todas las unidades de almacenamiento

        // Buscar archivos en todas las unidades del disco
        for (File root : roots) {
            buscarEnDirectorios(root, extensions);
        }
    }

    private void buscarEnDirectorios(File directory, ArrayList<String> extensions) {
        if (directory == null || !directory.isDirectory()) return;

        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                buscarEnDirectorios(file, extensions);
            } else {
            	// Si el archivo tiene la extensión correcta lo añade a la lista
                for (String ext : extensions) {
                    if (file.getName().toLowerCase().endsWith("." + ext)) {
                        listaModelo.addElement(file.getAbsolutePath());
                    }
                }
            }
        }
    }

    private void reproducirMusica() {
        if (cancionesLista.getSelectedValue() == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una canción para reproducir.");
            return;
        }

        String filePath = cancionesLista.getSelectedValue(); // Obtiene la ruta del archivo

        // Verifica si el archivo ya se está reproduciendo
        for (MiMusicaThread thread : activeThreads) {
            if (thread.getFilePath().equals(filePath) && thread.isAlive()) {
                JOptionPane.showMessageDialog(this, "Este archivo ya se está reproduciendo.");
                return;
            }
        }

        // Crea un nuevo hilo para reproducir el archivo
        MiMusicaThread thread = new MiMusicaThread(filePath);
        activeThreads.add(thread); // Añade el hilo a la lista de hilos activos
        thread.start(); // Inicia el hilo
    }

    private void pararMusica() {
        if (cancionesLista.getSelectedValue() == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una canción para detener.");
            return;
        }

        String filePath = cancionesLista.getSelectedValue();

        // Busca y detiene el hilo correspondiente al archivo seleccionado
        for (MiMusicaThread thread : activeThreads) {
            if (thread.getFilePath().equals(filePath) && thread.isAlive()) {
                thread.requestStop(); // Pide al hilo que se detenga
                JOptionPane.showMessageDialog(this, "Reproducción detenida para: " + filePath);
                return;
            }
        }

        JOptionPane.showMessageDialog(this, "El archivo seleccionado no se está reproduciendo.");
    }

    public static void main(String[] args) {
        new MiMusica();
    }
}

class MiMusicaThread extends Thread {
    private String filePath; // Ruta del archivo a reproducir
    private volatile boolean stopRequested; // Asegura que todos los hilos vean los cambios en la variable a tiempo real
    private Player player;

    public MiMusicaThread(String filePath) {
        this.filePath = filePath; // Guarda la ruta del archivo
        this.stopRequested = false; // Inicia la bandera de detención
    }

    public String getFilePath() {
        return filePath; // Devuelve la ruta del archivo
    }

    // Detiene la reproduccion del hilo cerrando el reproductor
    public void requestStop() {
        stopRequested = true; // Marca la bandera de detencion
        if (player != null) {
            player.close(); // Detiene la reproducción de inmediato
        }
    }

    @Override
    public void run() {
        try (FileInputStream fis = new FileInputStream(filePath)) { // Intenta abrir el archivo 
            player = new Player(fis); // Crea el reproductor de musica

            // Reproduce el archivo mientras no se solicite detener
            while (!stopRequested) {
                player.play(); // Reproduce la musica
            }
        } catch (Exception e) {
        	JOptionPane.showMessageDialog(null, "Error al reproducir el archivo: " + filePath, "Error", JOptionPane.ERROR_MESSAGE);
        } 
    } 
}
