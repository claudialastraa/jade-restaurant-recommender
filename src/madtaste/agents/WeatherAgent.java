package madtaste.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class WeatherAgent extends Agent {

    @Override
    protected void setup() {
        System.out.println("WeatherAgent iniciado: " + getLocalName());

        registrarServicio();

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {

                MessageTemplate filtro = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage mensaje = receive(filtro);

                if (mensaje != null) {
                    System.out.println("WeatherAgent ha recibido una petición de clima");

                    String clima = "soleado;24;false";

                    ACLMessage respuesta = mensaje.createReply();
                    respuesta.setPerformative(ACLMessage.INFORM);
                    respuesta.setContent(clima);

                    send(respuesta);

                    System.out.println("WeatherAgent ha respondido con: " + clima);
                } else {
                    block();
                }
            }
        });
    }

    private void registrarServicio() {
        DFAgentDescription descripcionAgente = new DFAgentDescription();
        descripcionAgente.setName(getAID());

        ServiceDescription servicio = new ServiceDescription();
        servicio.setType("weather-service");
        servicio.setName("MADTASTE-weather");

        descripcionAgente.addServices(servicio);

        try {
            DFService.register(this, descripcionAgente);
            System.out.println("WeatherAgent registrado en el DF con servicio weather-service");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println("WeatherAgent eliminado del DF");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}