package twin.developers.proyectonacionalfabian;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Mqtt {
    private static final String TAG = "MQTT";
    private static final String MQTT_SERVER = "tcp://broker.emqx.io:1883";
    private static final String CLIENT_ID = "mqttx_17947d6a";
    private static final String TOPIC_COLOR = "Android/Colores/Codigos";
    private static final int QOS = 2;

    private MqttAndroidClient mqttClient;
    private ColorChangeListener colorChangeListener;
    private DatabaseReference databaseReference;

    public interface ColorChangeListener {
        void onColorChanged(String colorHex);
    }

    public Mqtt(Context context, ColorChangeListener listener) {
        String clientId = CLIENT_ID + System.currentTimeMillis();
        String serverUri = MQTT_SERVER;
        this.colorChangeListener = listener;

        mqttClient = new MqttAndroidClient(context.getApplicationContext(), serverUri, clientId, new MemoryPersistence());
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.d(TAG, "Connection lost: " + cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                if (topic.equals(TOPIC_COLOR) && colorChangeListener != null) {
                    String colorHex = new String(message.getPayload());
                    colorChangeListener.onColorChanged(colorHex);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d(TAG, "Message delivered");
            }
        });

        // Inicializa la referencia a la base de datos
        databaseReference = FirebaseDatabase.getInstance().getReference("Codigo de Colores");
    }

    public void connectToMqttBroker() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);

        try {
            mqttClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Connected to MQTT broker");
                    subscribeToColorTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Failed to connect to MQTT broker: " + exception.getMessage());
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publishColor(String colorHex) {
        try {
            MqttMessage message = new MqttMessage(colorHex.getBytes());
            message.setQos(QOS);
            mqttClient.publish(TOPIC_COLOR, message);
            // Guarda el color en Firebase
            saveColorToFirebase(colorHex);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void subscribeToColorTopic() {
        try {
            mqttClient.subscribe(TOPIC_COLOR, QOS, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Subscribed to topic: " + TOPIC_COLOR);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Failed to subscribe to topic: " + TOPIC_COLOR);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void saveColorToFirebase(String colorHex) {
        // Guarda el color en la base de datos de Firebase
        String key = databaseReference.push().getKey();
        if (key != null) {
            databaseReference.child(key).setValue(colorHex);
        }
    }

    public void disconnect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    // AsyncTask para la conexión MQTT
    public static class MqttConnectionTask extends AsyncTask<Void, Void, Void> {
        private final Context context;

        public MqttConnectionTask(Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        protected Void doInBackground(Void... params) {
            // Pasa el contexto de la actividad al constructor de Mqtt
            Mqtt mqttManager = new Mqtt(context, null); // El segundo parámetro es un ColorChangeListener, que no es necesario aquí
            mqttManager.connectToMqttBroker();
            return null;
        }
    }
}
