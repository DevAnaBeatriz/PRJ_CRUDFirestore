package com.example.prj_firebase

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.prj_firebase.ui.theme.PRJ_FirebaseTheme

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

class MainActivity : ComponentActivity() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) } // Verifica se o usuário já está logado

            PRJ_FirebaseTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { paddingValues ->
                    if (isLoggedIn) {
                        App(db)
                    } else {
                        LoginScreen(auth) { loginSuccess ->
                            isLoggedIn = loginSuccess
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(auth: FirebaseAuth, onLoginSuccess: (Boolean) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Login", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                isLoading = true
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            message = "Login realizado com sucesso"
                            onLoginSuccess(true) // Chama o callback para indicar sucesso no login
                        } else {
                            message = "Erro no login: ${task.exception?.message}"
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                isLoading = true
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        message = if (task.isSuccessful) {
                            "Conta criada com sucesso"
                        } else {
                            "Erro ao criar conta: ${task.exception?.message}"
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Registrar")
        }

        Spacer(modifier = Modifier.height(8.dp))

        message?.let {
            Text(text = it, color = MaterialTheme.colorScheme.primary)
        }
    }
}



@Composable
fun App(db: FirebaseFirestore) {
    var nome by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var editingClienteId by remember { mutableStateOf<String?>(null) }

    val clientes = remember { mutableStateListOf<HashMap<String, String>>() }

    // Carregando dados do Firestore
    LaunchedEffect(Unit) {
        val listenerRegistration = db.collection("Clientes")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                clientes.clear()
                for (document in snapshots!!) {
                    val cliente = hashMapOf(
                        "id" to document.id,
                        "nome" to "${document.getString("nome") ?: "--"}",
                        "telefone" to "${document.data["telefone"] ?: "--"}"
                    )
                    clientes.add(cliente)
                }
            }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Título da página
        Text(
            text = "Agenda de Contatos",
            modifier = Modifier.padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )

        // Se estamos editando, mostrar o campo para edição
        if (isEditing) {
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Editar Nome") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = telefone,
                onValueChange = {
                    telefone = it
                },
                label = { Text("Editar Telefone") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // Atualizar o cliente no Firestore
                    editingClienteId?.let { id ->
                        db.collection("Clientes").document(id)
                            .update(mapOf(
                                "nome" to nome,
                                "telefone" to telefone
                            ))
                            .addOnSuccessListener {
                                Log.d(TAG, "Cliente atualizado com sucesso")
                                // Limpar os campos após a edição
                                nome = ""
                                telefone = ""
                                isEditing = false
                                editingClienteId = null
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Erro ao atualizar cliente", e)
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar Alterações")
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            // Campos de Nome e Telefone para novo cadastro
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Nome") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = telefone,
                onValueChange = {
                    telefone = it
                },
                label = { Text("Telefone") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Telefone") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Botão de Cadastrar
            Button(
                onClick = {
                    val pessoa = hashMapOf(
                        "nome" to nome,
                        "telefone" to telefone
                    )
                    db.collection("Clientes").add(pessoa)
                        .addOnSuccessListener { documentReference ->
                            Log.d(TAG, "DocumentSnapshot written with ID: ${documentReference.id}")
                            // Limpar os campos após o cadastro
                            nome = ""
                            telefone = ""
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error writing document", e)
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cadastrar")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Lista de clientes com ícones de editar e deletar
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(clientes) { cliente ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(0.4f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = "Nome")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = cliente["nome"] ?: "--")
                        }
                    }
                    Column(modifier = Modifier.weight(0.4f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = "Telefone")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = cliente["telefone"] ?: "--")
                        }
                    }
                    IconButton(
                        onClick = {
                            // Preparar a edição com os dados do cliente
                            nome = cliente["nome"] ?: ""
                            telefone = cliente["telefone"] ?: ""
                            isEditing = true
                            editingClienteId = cliente["id"]
                        },
                        modifier = Modifier.weight(0.1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = Color.Gray
                        )
                    }
                    IconButton(
                        onClick = {
                            // Deletar o cliente no Firestore
                            cliente["id"]?.let { id ->
                                db.collection("Clientes").document(id)
                                    .delete()
                                    .addOnSuccessListener {
                                        Log.d(TAG, "Cliente deletado com sucesso")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w(TAG, "Erro ao deletar cliente", e)
                                    }
                            }
                        },
                        modifier = Modifier.weight(0.1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Deletar",
                            tint = Color.Red
                        )
                    }
                }
                Divider()
            }
        }
    }
}





@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PRJ_FirebaseTheme {

    }
}