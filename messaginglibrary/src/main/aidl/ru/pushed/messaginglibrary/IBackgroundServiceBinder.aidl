// IBackgroundServiceBinder.aidl
package ru.pushed.messaginglibrary;

import ru.pushed.messaginglibrary.IBackgroundService;

interface IBackgroundServiceBinder {
     void bind(int id, IBackgroundService service);
     void unbind(int id);
     void invoke(String data);
}