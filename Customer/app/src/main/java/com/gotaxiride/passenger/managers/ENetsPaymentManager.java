package com.gotaxiride.passenger.managers;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.nets.enets.exceptions.InvalidPaymentRequestException;
import com.nets.enets.listener.PaymentCallback;
import com.nets.enets.network.PaymentRequestManager;
import com.nets.enets.utils.result.DebitCreditPaymentResponse;
import com.nets.enets.utils.result.NETSError;
import com.nets.enets.utils.result.NonDebitCreditPaymentResponse;
import com.nets.enets.utils.result.PaymentResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class ENetsPaymentManager
{

   //         ENetsPaymentManager.makePayment("","",this); this for user UI input function

    private static final String DEFAULT_KEY = "154eb31c-0f72-45bb-9249-84a1036fd1ca";
    private static final String DEFAULT_SECRET_KEY = "38a4b473-0295-439d-92e1-ad26a8c60279";
    private static final String DEFAULT_UMID = "UMID_877772003";
    private static final String DEFAULT_TXN_VALUE = "1000";

    public static void makePayment(String umid, String value, AppCompatActivity callerActivity)
    {
        String txnReq = "{\"ss\":\"1\",\"msg\":{\"netsMid\":\""+umid+"\",\"tid\":\"\",\"submissionMode\":\"B\",\"txnAmount\":\""+value+"\",\"merchantTxnRef\":\""+getTime()+"\",\"merchantTxnDtm\":\""+getTime()+".000\",\"paymentType\":\"SALE\",\"currencyCode\":\"SGD\",\"paymentMode\":\"\",\"merchantTimeZone\":\"+8:00\",\"b2sTxnEndURL\":\"https://sit2.enets.sg/MerchantApp/sim/b2sTxnEndURL.jsp\",\"b2sTxnEndURLParam\":\"\",\"s2sTxnEndURL\":\"https://sit2.enets.sg/MerchantApp/rest/s2sTxnEnd\",\"s2sTxnEndURLParam\":\"\",\"clientType\":\"S\",\"supMsg\":\"\",\"netsMidIndicator\":\"U\",\"ipAddress\":\"127.0.0.1\",\"language\":\"en\",\"mobileOs\":\"ANDROID\"}}";

        startPayment(txnReq,DEFAULT_SECRET_KEY,DEFAULT_KEY,callerActivity);
    }

    private static void startPayment(String txnReq, final String sKey, String key, final AppCompatActivity callerActivity)
    {
        // Generate HMAC
        String hmac = HMAC_Gen.generateSignature(txnReq, sKey);

        Log.d("startPayment", "txn: " + txnReq);
        Log.d("startPayment", "DEFAULT_SECRET_KEY: " + sKey);
        Log.d("startPayment", "hmac: " + hmac);

        PaymentRequestManager manager = PaymentRequestManager.getSharedInstance();
        try {
            manager.sendPaymentRequest(key, hmac, txnReq, new PaymentCallback()
            {
                @Override
                public void onResult(PaymentResponse paymentResponse)
                {
                    // To implement callback functions
                    if (paymentResponse instanceof DebitCreditPaymentResponse)
                    {
                        final DebitCreditPaymentResponse debitCreditPaymentResponse = (DebitCreditPaymentResponse) paymentResponse;
                        String txnRes = debitCreditPaymentResponse.txnResp;
                        String hmac = debitCreditPaymentResponse.hmac;
                        String keyId = debitCreditPaymentResponse.keyId;

                        Log.d("DebitCreditPaymentResponse", "txnRes: " + txnRes);
                        Log.d("DebitCreditPaymentResponse", "hmac: " + hmac);
                        Log.d("DebitCreditPaymentResponse", "keyId: " + keyId);

                        // Next 4 lines show a simplified verification.
                        // Basically checking if the hmac returned tallies with a hmac generated by our secret key
                        String hmacVerification = HMAC_Gen.generateSignature(txnRes, sKey);

                        Log.d("DebitCreditPaymentResponse", "hmacVerification: " + hmacVerification);

                        if (hmacVerification.equals(hmac))
                        {
                            Log.d("DebitCreditPaymentResponse", "Verification Successful");
                        }

                        try {
                            JSONObject txnJSON = new JSONObject(txnRes);
                            JSONObject msg = txnJSON.getJSONObject("msg");
                            String stageRespCode = msg.getString("stageRespCode");
                            Toast.makeText(callerActivity, "Payment Success\nstageRespCode: " + stageRespCode, Toast.LENGTH_LONG).show();
                            Log.d("DebitCreditPaymentResponse", "stageRespCode: " + stageRespCode);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                        // NETSPay payment will result in this callback.
                    } else if (paymentResponse instanceof NonDebitCreditPaymentResponse) {
                        final NonDebitCreditPaymentResponse nonDebitCreditPaymentResponse = (NonDebitCreditPaymentResponse) paymentResponse;

                        String txn_Status = nonDebitCreditPaymentResponse.status;
                        Log.d("nonDebitCreditPaymentResponse", "txn_Status: " + txn_Status);
                        Toast.makeText(callerActivity, "nonDebitCreditPaymentResponse Payment Success", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(NETSError netsError) {
                    String txn_ResponseCode = netsError.responeCode;
                    String txn_ActionCode = netsError.actionCode;

                    Log.d("netsError", "txn_ResponseCode: " + txn_ResponseCode);
                    Log.d("netsError", "txn_ActionCode: " + txn_ActionCode);

                }
            }, callerActivity);
        } catch (InvalidPaymentRequestException e) {
            e.printStackTrace();
            Log.e("InvalidPaymentException", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Exception", e.getMessage());
        }

    }

    // To create date&time string in proper format.
    public static String getTime(){
        Date date = new Date();
        String formattedTime = String.format("%d%02d%02d %02d:%02d:%02d", date.getYear()+1900, date.getMonth()+1, date.getDate(), date.getHours(), date.getMinutes(), date.getSeconds());
        Log.d("Time", formattedTime);
        return formattedTime;
    }
}
