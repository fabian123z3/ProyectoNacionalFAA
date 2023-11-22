package twin.developers.proyectonacionalfabian;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import twin.developers.projectmqtt.R;
import yuku.ambilwarna.AmbilWarnaDialog;

public class MainActivity extends AppCompatActivity {

    private Button botonEnviar;
    private View colorSeleccionadoView;
    private int colorSeleccionado = Color.WHITE; // Color inicial
    private Mqtt mqttManager; // Agregamos la instancia de Mqtt

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        botonEnviar = findViewById(R.id.enviar);
        colorSeleccionadoView = findViewById(R.id.colorSeleccionadoView);

        // Cambio: Pasa el contexto de la actividad al constructor de Mqtt
        mqttManager = new Mqtt(getApplicationContext(), new Mqtt.ColorChangeListener() {
            @Override
            public void onColorChanged(String colorHex) {
                // Aquí puedes manejar el cambio de color si es necesario
            }
        });

        botonEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarColorPicker();
            }
        });

        // Cambio: Conecta a MQTT solo cuando la actividad se crea
        mqttManager.connectToMqttBroker();
    }

    public void mostrarColorPicker() {
        AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, colorSeleccionado, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                // Muestra el color seleccionado en el View debajo del botón
                colorSeleccionado = color;
                colorSeleccionadoView.setBackgroundColor(color);

                // Convierte el color a formato hexadecimal y lo publica en MQTT
                String colorHex = String.format("#%06X", (0xFFFFFF & color));
                mqttManager.publishColor(colorHex);
            }

            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
                // Acción al cancelar (puedes dejarlo vacío o realizar alguna acción)
            }
        });
        dialog.show();
    }
}