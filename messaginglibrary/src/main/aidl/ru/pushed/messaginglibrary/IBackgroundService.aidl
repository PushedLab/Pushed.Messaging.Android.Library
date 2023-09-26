// IBackgroundService.aidl
package ru.pushed.messaginglibrary;

interface IBackgroundService {
         boolean invoke(String data);
         void stop();
}