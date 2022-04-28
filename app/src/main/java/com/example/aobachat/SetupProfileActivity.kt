package com.example.aobachat

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ProgressBar
import com.example.aobachat.databinding.ActivitySetupProfileBinding
import com.example.aobachat.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class SetupProfileActivity : AppCompatActivity() {

    var binding : ActivitySetupProfileBinding? = null
    var auth : FirebaseAuth? = null
    var database : FirebaseDatabase? = null
    var storage : FirebaseStorage? = null
    var selectedImage : Uri? = null
    var dialog : ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupProfileBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        dialog?.setMessage("Atualizando Perfil...")
        dialog?.setCancelable(false)
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        supportActionBar?.hide()
        binding!!.imgView.setOnClickListener{
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(intent,45)
        }

        binding!!.btnContinue2.setOnClickListener{
            val name : String = binding!!.editName.text.toString()
            if(name.isEmpty()){
                binding!!.editName.setError("Por favor, digite seu nome")
            }
            dialog!!.show()
            if(selectedImage != null){
                val reference = storage!!.reference.child("Profile")
                    .child(auth!!.uid!!)
                reference.putFile(selectedImage!!).addOnCompleteListener{ task ->
                    if (task.isSuccessful){
                        reference.downloadUrl.addOnCompleteListener{ uri ->
                            val imageUrl = uri.toString()
                            val uid = auth!!.uid
                            val phone = auth!!.currentUser!!.phoneNumber
                            val name : String = binding!!.editName.text.toString()
                            val user = User(uid,name,phone,imageUrl)
                            database!!.reference
                                .child("users")
                                .child(uid!!)
                                .setValue(user)
                                .addOnCompleteListener{
                                    dialog!!.dismiss()
                                    val intent = Intent(this@SetupProfileActivity,MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                        }
                    }else{
                        val uid = auth!!.uid
                        val phone = auth!!.currentUser!!.phoneNumber
                        val name : String = binding!!.editName.text.toString()
                        val user = User(uid,name,phone,"Sem imagem")
                        database!!.reference
                            .child("users")
                            .child(uid!!)
                            .setValue(user)
                            .addOnCanceledListener {
                                dialog!!.dismiss()
                                val intent = Intent(this@SetupProfileActivity,MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                    }

                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(data !=  null){
            if(data.data != null){
                val uri = data.data //filePath
                val storage = FirebaseStorage.getInstance()
                val time = Date().time
                val reference = storage.reference
                    .child("Profile")
                    .child(time.toString() + "")
                reference.putFile(uri!!).addOnCompleteListener{ task ->
                    if(task.isSuccessful){
                        reference.downloadUrl.addOnCompleteListener{ uri ->
                            val filePath = uri.toString()
                            val obj = HashMap<String,Any>()
                            obj["image"]=filePath
                            database!!.reference
                                .child("users")
                                .child(FirebaseAuth.getInstance().uid!!)
                                .updateChildren(obj).addOnSuccessListener {  }
                        }
                    }
                }
                binding!!.imgView.setImageURI(data.data)
                selectedImage = data.data
            }
        }
    }
}