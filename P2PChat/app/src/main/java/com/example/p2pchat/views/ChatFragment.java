package com.example.p2pchat.views;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.example.p2pchat.App;
import com.example.p2pchat.MainActivity;
import com.example.p2pchat.R;
import com.example.p2pchat.adapters.MessagesRecyclerViewAdapter;
import com.example.p2pchat.data.Database;
import com.example.p2pchat.data.model.Message;
import com.example.p2pchat.data.model.Session;
import com.example.p2pchat.data.model.helperModel.SessionWithMessageCount;
import com.example.p2pchat.viewModels.ChatFragmentViewModel;

import java.util.List;

import io.reactivex.functions.Function;

public class ChatFragment extends Fragment {
    private static final String TAG = "ChatFragment";
    MainActivity mainActivity;
    RecyclerView recyclerView;
    MessagesRecyclerViewAdapter recyclerAdapter;
    ChatFragmentViewModel chatFragmentViewModel;
    Button sendButton;
    EditText messageText;
    ImageButton backButton;
    ImageButton deleteButton;
    TextView phoneName;
    NavController navController;

    public ChatFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = ((MainActivity)getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view;
        view = inflater.inflate(R.layout.fragment_chat, container, false);
        recyclerView = view.findViewById(R.id.recyclerView_chatMessages);
        sendButton = view.findViewById(R.id.button_messageSend);
        messageText = view.findViewById(R.id.editText_messageInput);
        backButton = view.findViewById(R.id.imageButton_chatBack);
        deleteButton = view.findViewById(R.id.imageButton_deleteThisMessage);
        phoneName = view.findViewById(R.id.textView_peerPhoneName);

        Boolean isHistoryMode = getArguments().getBoolean("HistoryMode");
        if (isHistoryMode) {
            messageText.setVisibility(View.GONE);
            sendButton.setVisibility(View.GONE);
        } else {
            deleteButton.setVisibility(View.INVISIBLE);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chatFragmentViewModel = ViewModelProviders.of(this).get(ChatFragmentViewModel.class);
        navController = Navigation.findNavController(this.getView());

        Long sessionId = getArguments().getLong("SessionId");
        String peerMac = getArguments().getString("PeerMac");
        Log.d(TAG, "onViewCreated: arguments: " + getArguments());
        Log.d(TAG, "onViewCreated: SESSION ID ARIS: " + sessionId);
        Log.d(TAG, "onViewCreated: peerMac: " + peerMac);
        if (peerMac != null){
            Log.d(TAG, "onViewCreated: inside if: mac:" + peerMac);
            chatFragmentViewModel.init(peerMac, this);
        } else {
            Log.d(TAG, "onViewCreated: inside if: ID:" + sessionId);
            chatFragmentViewModel.init(sessionId);
        }
        initOnClickListeners();
        initDataObservers();
        initRecyclerView();
    }

    private void initOnClickListeners() {
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String messageStr = messageText.getText().toString().trim();
                if(messageStr.equals("")){
                    return;
                } else {
                    chatFragmentViewModel.sendMessage(messageStr);
                    messageText.setText("");
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getArguments().getBoolean("HistoryMode") == true){
                    navController.navigateUp();
                } else {
                    popUpDialogue("Yes", "Do you want to disconnect from peer?"
                            , new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //TODO: implement disconnect
                                    closeChatSession();
                                }
                            }, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            });
                }
            }
        });
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayDeletePopup();
            }
        });

    }

    private void closeChatSession(){
        mainActivity.removeConnection(new ConnectionListener() {
            @Override
            public void onDisconnect() {
                Log.d(TAG, "onDisconnect: NAVIGATING UP!!!");
//                navController.navigateUp();
            }
        });
    }

    private void initDataObservers() {
        chatFragmentViewModel.getMessages().observe(this, new Observer<List<Message>>() {
            @Override
            public void onChanged(List<Message> messages) {
                Log.d(TAG, "onChanged: messages changed to: " + messages);
                recyclerAdapter.updateDataSet(messages);
                if(messages!=null && messages.size()>0){
                    recyclerView.smoothScrollToPosition(messages.size()-1);
                }
            }
        });
        chatFragmentViewModel.getSession().observe(this, new Observer<Session>() {
            @Override
            public void onChanged(Session session) {
                Log.d(TAG, "onChanged: session changed to: " + session);
                if (session == null) {
//                    navController.navigateUp();
                }
                chatFragmentViewModel.setSessionId(session.getSessionId());
            }
        });
        LiveData<SessionWithMessageCount> s;
        s = Database.getInstance().dataDao().getSessionWithMessageCount(chatFragmentViewModel.getSessionId());
        s.observe(this, new Observer<SessionWithMessageCount>() {
            @Override
            public void onChanged(SessionWithMessageCount sessionWithMessageCount) {
                if(sessionWithMessageCount != null) {
                    phoneName.setText(sessionWithMessageCount.getPeerPhoneName());
                    Log.d(TAG, "onChanged: session with message count: " + sessionWithMessageCount);
                }
            }
        });
    }

    private void displayDeletePopup() {
        popUpDialogue("Yes",
                "Do you want to delete this Chat?",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        chatFragmentViewModel.deleteThisSession();
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
    }

    public AlertDialog popUpDialogue(String positiveLabel,
                                     String popupMessage,
                                     DialogInterface.OnClickListener positiveOnClick,
                                     DialogInterface.OnClickListener negativeOnClick) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(true);
        builder.setMessage(popupMessage);
        builder.setPositiveButton(positiveLabel, positiveOnClick);
        builder.setNegativeButton("Cancel", negativeOnClick);

        AlertDialog alert = builder.create();
        alert.show();
        return alert;
    }

    private void initRecyclerView() {
        recyclerAdapter = new MessagesRecyclerViewAdapter(chatFragmentViewModel.getMessages().getValue());
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(App.getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(recyclerAdapter);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public interface ConnectionListener{
        void onDisconnect();
    }
}
