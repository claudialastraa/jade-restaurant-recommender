package madtaste.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InterfaceAgent extends Agent {

    private JFrame ventana;
    private JTextField campoTipo;
    private JTextField campoPresupuesto;
    private JTextField campoDistancia;
    private JComboBox<String> comboLugar;
    private JTextArea areaResultados;

    @Override
    protected void setup() {
        System.out.println("InterfaceAgent iniciado: " + getLocalName());

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                crearInterfaz();
            }
        });
    }

    private void crearInterfaz() {
        ventana = new JFrame("MADTASTE - Recomendador de Restaurantes");
        ventana.setSize(750, 550);
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.setLocationRelativeTo(null);
        ventana.setLayout(new BorderLayout(10, 10));

        JLabel titulo = new JLabel("MADTASTE", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 26));

        JLabel subtitulo = new JLabel("Sistema multiagente de recomendación de restaurantes en Madrid", SwingConstants.CENTER);
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 14));

        JPanel panelTitulo = new JPanel(new GridLayout(2, 1));
        panelTitulo.setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));
        panelTitulo.add(titulo);
        panelTitulo.add(subtitulo);

        ventana.add(panelTitulo, BorderLayout.NORTH);

        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JPanel panelFormulario = new JPanel(new GridLayout(5, 2, 10, 10));
        panelFormulario.setBorder(BorderFactory.createTitledBorder("Preferencias del usuario"));

        panelFormulario.add(new JLabel("Tipo de comida:"));
        campoTipo = new JTextField("italiana");
        panelFormulario.add(campoTipo);

        panelFormulario.add(new JLabel("Presupuesto máximo (€):"));
        campoPresupuesto = new JTextField("20");
        panelFormulario.add(campoPresupuesto);

        panelFormulario.add(new JLabel("Distancia máxima (km):"));
        campoDistancia = new JTextField("3");
        panelFormulario.add(campoDistancia);

        panelFormulario.add(new JLabel("Preferencia de lugar:"));
        comboLugar = new JComboBox<String>();
        comboLugar.addItem("terraza");
        comboLugar.addItem("interior");
        comboLugar.addItem("indiferente");
        panelFormulario.add(comboLugar);

        JButton botonRecomendar = new JButton("Obtener recomendaciones");
        botonRecomendar.setFont(new Font("Arial", Font.BOLD, 14));

        JButton botonLimpiar = new JButton("Limpiar resultados");

        panelFormulario.add(botonRecomendar);
        panelFormulario.add(botonLimpiar);

        panelPrincipal.add(panelFormulario, BorderLayout.NORTH);

        areaResultados = new JTextArea();
        areaResultados.setEditable(false);
        areaResultados.setFont(new Font("Monospaced", Font.PLAIN, 13));
        areaResultados.setText(
                "Introduce tus preferencias y pulsa \"Obtener recomendaciones\".\n\n" +
                "Ejemplo:\n" +
                "- Tipo de comida: italiana\n" +
                "- Presupuesto: 20\n" +
                "- Distancia: 3\n" +
                "- Lugar: terraza\n"
        );

        JScrollPane scrollResultados = new JScrollPane(areaResultados);
        scrollResultados.setBorder(BorderFactory.createTitledBorder("Resultados"));

        panelPrincipal.add(scrollResultados, BorderLayout.CENTER);

        ventana.add(panelPrincipal, BorderLayout.CENTER);

        botonRecomendar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pedirRecomendaciones();
            }
        });

        botonLimpiar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                areaResultados.setText("");
            }
        });

        ventana.setVisible(true);
    }

    private void pedirRecomendaciones() {
        AID recommendationAgent = buscarAgentePorServicio("recommendation-service");

        if (recommendationAgent == null) {
            areaResultados.setText("ERROR: No se encontró el RecommendationAgent en el DF.");
            return;
        }

        String preferencias =
                campoTipo.getText().trim() + ";" +
                campoPresupuesto.getText().trim() + ";" +
                campoDistancia.getText().trim() + ";" +
                comboLugar.getSelectedItem().toString();

        ACLMessage peticion = new ACLMessage(ACLMessage.REQUEST);
        peticion.addReceiver(recommendationAgent);
        peticion.setContent(preferencias);

        send(peticion);

        areaResultados.setText(
                "Buscando recomendaciones...\n\n" +
                "Preferencias enviadas:\n" +
                "Tipo de comida: " + campoTipo.getText().trim() + "\n" +
                "Presupuesto máximo: " + campoPresupuesto.getText().trim() + " €\n" +
                "Distancia máxima: " + campoDistancia.getText().trim() + " km\n" +
                "Lugar preferido: " + comboLugar.getSelectedItem().toString() + "\n"
        );

        new Thread(new Runnable() {
            @Override
            public void run() {
                MessageTemplate filtro = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchSender(recommendationAgent)
                );

                ACLMessage respuesta = blockingReceive(filtro);

                if (respuesta != null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            areaResultados.setText(respuesta.getContent());
                        }
                    });
                }
            }
        }).start();
    }

    private AID buscarAgentePorServicio(String tipoServicio) {
        DFAgentDescription template = new DFAgentDescription();

        ServiceDescription servicio = new ServiceDescription();
        servicio.setType(tipoServicio);
        template.addServices(servicio);

        try {
            DFAgentDescription[] resultados = DFService.search(this, template);

            if (resultados.length > 0) {
                return resultados[0].getName();
            }

        } catch (FIPAException e) {
            e.printStackTrace();
        }

        return null;
    }
}