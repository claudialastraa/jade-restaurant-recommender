package madtaste.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RecommendationAgent extends Agent {

    @Override
    protected void setup() {
        System.out.println("RecommendationAgent iniciado: " + getLocalName());

        registrarServicio();

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {

                MessageTemplate filtro = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage mensajeUsuario = receive(filtro);

                if (mensajeUsuario != null) {
                    System.out.println("RecommendationAgent ha recibido preferencias: " + mensajeUsuario.getContent());

                    String preferencias = mensajeUsuario.getContent();

                    String clima = pedirInformacion("weather-service", "Dame clima");
                    String restaurantes = pedirInformacion("restaurant-data-service", "Dame restaurantes");

                    String recomendaciones = calcularRecomendaciones(preferencias, clima, restaurantes);

                    ACLMessage respuesta = mensajeUsuario.createReply();
                    respuesta.setPerformative(ACLMessage.INFORM);
                    respuesta.setContent(recomendaciones);

                    send(respuesta);

                    System.out.println("RecommendationAgent ha enviado recomendaciones");
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
        servicio.setType("recommendation-service");
        servicio.setName("MADTASTE-recommendation");

        descripcionAgente.addServices(servicio);

        try {
            DFService.register(this, descripcionAgente);
            System.out.println("RecommendationAgent registrado en el DF con servicio recommendation-service");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
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

    private String pedirInformacion(String tipoServicio, String contenido) {
        AID receptor = buscarAgentePorServicio(tipoServicio);

        if (receptor == null) {
            return "ERROR: No se encontró servicio " + tipoServicio;
        }

        ACLMessage peticion = new ACLMessage(ACLMessage.REQUEST);
        peticion.addReceiver(receptor);
        peticion.setContent(contenido);

        send(peticion);

        MessageTemplate filtroRespuesta = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchSender(receptor)
        );

        ACLMessage respuesta = blockingReceive(filtroRespuesta);

        if (respuesta != null) {
            return respuesta.getContent();
        }

        return "ERROR: Sin respuesta de " + tipoServicio;
    }

    private String calcularRecomendaciones(String preferencias, String clima, String restaurantes) {
        String[] partesPreferencias = preferencias.split(";");

        String tipoPreferido = partesPreferencias[0];
        double presupuestoMax = Double.parseDouble(partesPreferencias[1]);
        double distanciaMax = Double.parseDouble(partesPreferencias[2]);
        String preferenciaLugar = partesPreferencias[3];

        String[] partesClima = clima.split(";");

        String estadoClima = partesClima[0];
        boolean lluvia = Boolean.parseBoolean(partesClima[2]);

        List<Recomendacion> listaRecomendaciones = new ArrayList<>();

        String[] lineas = restaurantes.split("\n");

        for (String linea : lineas) {
            if (linea.trim().isEmpty()) {
                continue;
            }

            String[] datos = linea.split(";");

            String nombre = datos[0];
            String tipo = datos[1];
            double precio = Double.parseDouble(datos[2]);
            double distancia = Double.parseDouble(datos[3]);
            double valoracion = Double.parseDouble(datos[4]);
            boolean terraza = Boolean.parseBoolean(datos[5]);
            boolean interior = Boolean.parseBoolean(datos[6]);

            double puntuacion = valoracion * 2;

            if (tipo.equalsIgnoreCase(tipoPreferido)) {
                puntuacion += 3;
            }

            if (precio <= presupuestoMax) {
                puntuacion += 2;
            } else {
                puntuacion -= 2;
            }

            if (distancia <= distanciaMax) {
                puntuacion += 2;
            } else {
                puntuacion -= 1;
            }

            if (lluvia && interior) {
                puntuacion += 2;
            }

            if (!lluvia && terraza) {
                puntuacion += 1;
            }

            if (preferenciaLugar.equalsIgnoreCase("terraza") && terraza) {
                puntuacion += 1;
            }

            if (preferenciaLugar.equalsIgnoreCase("interior") && interior) {
                puntuacion += 1;
            }

            String texto =
                    nombre +
                    " | tipo: " + tipo +
                    " | precio: " + precio + "€" +
                    " | distancia: " + distancia + " km" +
                    " | valoración: " + valoracion +
                    " | puntuación: " + String.format("%.2f", puntuacion);

            listaRecomendaciones.add(new Recomendacion(texto, puntuacion));
        }

        listaRecomendaciones.sort(new Comparator<Recomendacion>() {
            @Override
            public int compare(Recomendacion r1, Recomendacion r2) {
                return Double.compare(r2.getPuntuacion(), r1.getPuntuacion());
            }
        });

        StringBuilder resultado = new StringBuilder();
        resultado.append("Clima actual: ").append(estadoClima).append("\n\n");
        resultado.append("Recomendaciones ordenadas por puntuación:\n");

        int posicion = 1;

        for (Recomendacion recomendacion : listaRecomendaciones) {
            resultado.append(posicion)
                    .append(". ")
                    .append(recomendacion.getTexto())
                    .append("\n");
            posicion++;
        }

        return resultado.toString();
    }

    private static class Recomendacion {
        private String texto;
        private double puntuacion;

        public Recomendacion(String texto, double puntuacion) {
            this.texto = texto;
            this.puntuacion = puntuacion;
        }

        public String getTexto() {
            return texto;
        }

        public double getPuntuacion() {
            return puntuacion;
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println("RecommendationAgent eliminado del DF");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}