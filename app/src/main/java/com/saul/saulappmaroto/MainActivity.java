package com.saul.saulappmaroto;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.tooltip.Tooltip;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class MainActivity extends Activity {

    NfcAdapter nfcAdapter;
    EditText edt_setpoint;
    EditText edt_histerese;
    EditText edt_tempo;
    Button btn_habilitar;
    Button infosetpoint;
    Button infohist;
    Button infotempo;
    Tooltip tt;
    boolean habilitado = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        edt_setpoint = findViewById(R.id.edt_setpoint);
        edt_histerese = findViewById(R.id.edt_histerese);
        edt_tempo = findViewById(R.id.edt_tempo);
        btn_habilitar = findViewById(R.id.btn_habilitar);
        infohist = findViewById(R.id.infohist);
        infohist.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View view) {
        showTooltip(view, R.string.info_histerese);
    }
});
        infosetpoint = findViewById(R.id.infosetpoint);
        infosetpoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTooltip(view,R.string.info_setpoint);
            }
        });
        infotempo = findViewById(R.id.infotempo);
        infotempo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTooltip(view,R.string.info_tempo);
            }
        });
        btn_habilitar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                habilitar_switch();
            }
        });
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

    }

    @Override
    protected void onResume() {
        super.onResume();

        enableForegroundDispatchSystem();
    }

    @Override
    protected void onPause() {
        super.onPause();

        disableForegroundDispatchSystem();
    }

    private void showTooltip(View btn,int message){
        if(tt!=null){
        if(tt.isShowing()){
            tt.dismiss();
        }}
        tt = new Tooltip.Builder(btn).setText(getString(message)).setTextColor(Color.WHITE).setBackgroundColor(Color.BLUE).setCornerRadius(15f).setDismissOnClick(true).show();
    }

    private String getMensagem() {
        String mensagem = "";
        int tempo = Integer.valueOf(edt_tempo.getText().toString());
        Double setpoint = Double.valueOf(edt_setpoint.getText().toString());
        Double histerese = Double.valueOf(edt_histerese.getText().toString());
        if (setpoint.intValue() < 10) {
            mensagem += "0" + String.valueOf(setpoint.intValue());
        } else {
            mensagem += String.valueOf(setpoint.intValue());
        }
        setpoint = (setpoint - setpoint.intValue()) * 10;
        mensagem += setpoint.intValue();
        if (histerese.intValue() < 10) {
            mensagem += "0" + String.valueOf(histerese.intValue());
        } else {
            mensagem += String.valueOf(histerese.intValue());
        }
        histerese = (histerese - histerese.intValue()) * 10;
        mensagem += histerese.intValue();
        if (tempo < 10) {
            mensagem += "00" + String.valueOf(tempo);
        }else{
            mensagem += "0" + String.valueOf(tempo);
        }
        return mensagem;
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {
            Toast.makeText(this, "Tag detectada!", Toast.LENGTH_SHORT).show();
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if(habilitado) {
                NdefMessage ndefMessage = createNdefMessage(getMensagem());
                writeNdefMessage(tag, ndefMessage);
            }
        }
    }


    private void enableForegroundDispatchSystem() {

        Intent intent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        IntentFilter[] intentFilters = new IntentFilter[]{};

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null);
    }

    private void disableForegroundDispatchSystem() {
        nfcAdapter.disableForegroundDispatch(this);
    }

    private void formatTag(Tag tag, NdefMessage ndefMessage) {
        try {

            NdefFormatable ndefFormatable = NdefFormatable.get(tag);

            if (ndefFormatable == null) {
                Toast.makeText(this, "Tag is not ndef formatable!", Toast.LENGTH_SHORT).show();
                return;
            }


            ndefFormatable.connect();
            ndefFormatable.format(ndefMessage);
            ndefFormatable.close();

            Toast.makeText(this, "Tag writen!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e("formatTag", e.getMessage());
        }

    }

    private void writeNdefMessage(Tag tag, NdefMessage ndefMessage) {

        try {

            if (tag == null) {
                Toast.makeText(this, "Tag object cannot be null", Toast.LENGTH_SHORT).show();
                return;
            }

            Ndef ndef = Ndef.get(tag);

            if (ndef == null) {
                // format tag with the ndef format and writes the message.
                formatTag(tag, ndefMessage);
            } else {
                ndef.connect();

                if (!ndef.isWritable()) {
                    Toast.makeText(this, "Tag is not writable!", Toast.LENGTH_SHORT).show();

                    ndef.close();
                    return;
                }

                ndef.writeNdefMessage(ndefMessage);
                ndef.close();
                Toast.makeText(this, "Dados enviados!", Toast.LENGTH_SHORT).show();
                testBeep();
            }

        } catch (Exception e) {
            Log.e("writeNdefMessage", e.getMessage());
        }

    }


    private NdefRecord createTextRecord(String content) {
        try {
            byte[] language;
            language = Locale.getDefault().getLanguage().getBytes("UTF-8");

            final byte[] text = content.getBytes("UTF-8");
            final int languageSize = language.length;
            final int textLength = text.length;
            final ByteArrayOutputStream payload = new ByteArrayOutputStream(1 + languageSize + textLength);

            payload.write((byte) (languageSize & 0x1F));
            payload.write(language, 0, languageSize);
            payload.write(text, 0, textLength);

            return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload.toByteArray());

        } catch (UnsupportedEncodingException e) {
            Log.e("createTextRecord", e.getMessage());
        }
        return null;
    }


    private NdefMessage createNdefMessage(String content) {

        NdefRecord ndefRecord = createTextRecord(content);

        NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{ndefRecord});

        return ndefMessage;
    }

    private void testBeep() {
        ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        toneGen1.startTone(ToneGenerator.TONE_DTMF_D, 150);
    }

    private void habilitar_switch() {
        if(check_edt()){
            habilitado = !habilitado;
            if (habilitado) {
                btn_habilitar.setBackgroundColor(Color.RED);
                btn_habilitar.setText(R.string.main_btn_desabilitar_text);
            } else {
                btn_habilitar.setBackgroundColor(Color.GREEN);
                btn_habilitar.setText(R.string.main_btn_habilitar_text);
            }
        }
    }

    private boolean check_edt(){
        if(edt_setpoint.getText().toString().isEmpty()){
            edt_setpoint.setError("Nao pode ser vazio",getDrawable(R.mipmap.warning));
            edt_setpoint.requestFocus();
            return false;
        }else if(Double.valueOf(edt_setpoint.getText().toString())<10 ||Double.valueOf(edt_setpoint.getText().toString())>99.9){
            edt_setpoint.setError("Valor não esta no intervalo permitido!",getDrawable(R.mipmap.warning));
            edt_setpoint.requestFocus();
            return false;
        }
        if(edt_histerese.getText().toString().isEmpty()){
            edt_histerese.setError("Nao pode ser vazio",getDrawable(R.mipmap.warning));
            edt_histerese.requestFocus();
            return false;
        }else if(Double.valueOf(edt_histerese.getText().toString())<0.1||Double.valueOf(edt_histerese.getText().toString())>10){
            edt_histerese.setError("Valor não esta no intervalo permitido!",getDrawable(R.mipmap.warning));
            edt_histerese.requestFocus();
            return false;
        }
        if(edt_tempo.getText().toString().isEmpty()){
            edt_tempo.setError("Nao pode ser vazio",getDrawable(R.mipmap.warning));
            edt_tempo.requestFocus();
            return false;
        }else if(Integer.valueOf(edt_tempo.getText().toString())<1||Integer.valueOf(edt_tempo.getText().toString())>24){
            edt_tempo.setError("Valor não esta no intervalo permitido!",getDrawable(R.mipmap.warning));
            edt_tempo.requestFocus();
            return false;
        }
        return true;
    }

}
