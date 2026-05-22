package madtaste.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class RestaurantDataAgent extends Agent {

    @Override
    protected void setup() {
        System.out.println("RestaurantDataAgent iniciado: " + getLocalName());

        registrarServicio();

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {

                MessageTemplate filtro = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage mensaje = receive(filtro);

                if (mensaje != null) {
                    System.out.println("RestaurantDataAgent ha recibido una petición de restaurantes");

                    String restaurantes = leerRestaurantesCSV();

                    ACLMessage respuesta = mensaje.createReply();
                    respuesta.setPerformative(ACLMessage.INFORM);
                    respuesta.setContent(restaurantes);

                    send(respuesta);

                    System.out.println("RestaurantDataAgent ha enviado restaurantes desde CSV");
                } else {
                    block();
                }
            }
        });
    }

    private String leerRestaurantesCSV() {
        StringBuilder contenido = new StringBuilder();

        String ruta = "data/restaurantes.csv";

        try (BufferedReader br = new BufferedReader(new FileReader(ruta))) {
            String linea;
            boolean primeraLinea = true;

            while ((linea = br.readLine()) != null) {
                if (primeraLinea) {
                    primeraLinea = false;
                    continue;
                }

                String lineaConvertida = linea.replace(",", ";");
                contenido.append(lineaConvertida).append("\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR;csv;0;0;0;false;false";
        }

        return contenido.toString();
    }

    private void registrarServicio() {
        DFAgentDescription descripcionAgente = new DFAgentDescription();
        descripcionAgente.setName(getAID());

        ServiceDescription servicio = new ServiceDescription();
        servicio.setType("restaurant-data-service");
        servicio.setName("MADTASTE-restaurant-data");

        descripcionAgente.addServices(servicio);

        try {
            DFService.register(this, descripcionAgente);
            System.out.println("RestaurantDataAgent registrado en el DF con servicio restaurant-data-service");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println("RestaurantDataAgent eliminado del DF");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}