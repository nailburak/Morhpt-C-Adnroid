package com.morhpt.c.help;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;

import com.morhpt.c.FCMActivity;
import com.morhpt.c.MainActivity;
import com.morhpt.c.Messages;
import com.morhpt.c.R;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

/**
 * Created by nbura on 2017-08-27.
 */

public class JavaHelp {
    public static Bitmap decodeUri(Context c, Uri uri, final int requiredSize)
            throws FileNotFoundException {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(c.getContentResolver().openInputStream(uri), null, o);

        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;

        while (true) {
            if (width_tmp / 2 < requiredSize || height_tmp / 2 < requiredSize)
                break;
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(c.getContentResolver().openInputStream(uri), null, o2);
    }

    public static byte[] getImageData(Bitmap bmp) {

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();

    }

    public static void notif(Context context) {

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {

            Log.wtf("hey", "O degil");

            try {
                URL url = new URL("https://organicthemes.com/demo/profile/files/2012/12/profile_img.png");
                Bitmap image = BitmapFactory.decodeStream(url.openConnection().getInputStream());

                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setLargeIcon(image)
                        .setContentTitle("title")
                        .setContentText("msg")
                        .setAutoCancel(true);

                android.app.NotificationManager notificationManager =
                        (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());


            } catch (IOException e) {
                System.out.println(e);
            }

        } else {

            try {
                URL url = new URL("https://organicthemes.com/demo/profile/files/2012/12/profile_img.png");
                Bitmap image = BitmapFactory.decodeStream(url.openConnection().getInputStream());

                Log.wtf("hey", "its Oreo");


                final String KEY_TEXT_REPLY = "key_text_reply";
                String replyLabel = "REPLY";

                Intent resultIntent = new Intent(context, FCMActivity.class);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
// Adds the back stack for the Intent (but not the Intent itself)
                // stackBuilder.addParentStack(FCMActivity.class);
// Adds the Intent that starts the Activity to the top of the stack
                stackBuilder.addNextIntent(resultIntent);

                RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_REPLY)
                        .setLabel(replyLabel)
                        .build();

                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(
                                0,
                                PendingIntent.FLAG_ONE_SHOT
                        );


                Notification.Action action =
                        new Notification.Action.Builder(R.drawable.ic_send_white_24dp,
                                "label", resultPendingIntent)
                                .addRemoteInput(remoteInput)
                                //.setChannelId("msg")
                                .build();



// Issue the notification.


                NotificationManager mNotificationManager =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
// The id of the channel.
                String id = "my_channel_01";
// The user-visible name of the channel.
                CharSequence name = "name";
// The user-visible description of the channel.
                String description ="desc";
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel mChannel = new NotificationChannel(id, name, importance);
// Configure the notification channel.
                mChannel.setDescription(description);
                mChannel.enableLights(true);
// Sets the notification light color for notifications posted to this
// channel, if the device supports this feature.
                mChannel.setLightColor(Color.RED);
                mChannel.enableVibration(true);
                mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
                mNotificationManager.createNotificationChannel(mChannel);


                Notification.Builder mBuilder = new Notification.Builder(context, id)
                        .setContentTitle("Content")
                        .setContentText("BOdyyy")
                        .setLargeIcon(image)
                        .addAction(action)
                        .setSmallIcon(R.drawable.logo)
                        .setAutoCancel(true);

                mNotificationManager.notify(1, mBuilder.build());


            } catch (IOException e){
                e.printStackTrace();
            }

        }
    }
}
