package com.example.tp_listview

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    // 1) Les données : une liste MODIFIABLE (mutableListOf) de chaînes simples
    private val contacts = mutableListOf(
        "Ahmed Ben Ali",
        "Sarra Trabelsi",
        "Mohamed Gharbi",
        "Ines Jaziri",
        "Youssef Mansouri",
        "Amira Chaabane",
        "Karim Bouazizi",
        "Nour Hammami"
    )

    // 2) L'adaptateur fait le lien entre les données et la ListView
    private lateinit var adapter: ArrayAdapter<String>

    // 3) Les vues
    private lateinit var etContact: EditText
    private lateinit var btnModifier: Button
    private lateinit var btnAjouter: Button
    private lateinit var lvContacts: ListView

    // Position de l'élément sélectionné (-1 = aucune sélection)
    private var positionSelectionnee = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Laisse la place aux barres système (barre d'état, clavier, navigation)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // Récupération des vues déclarées dans activity_main.xml
        etContact = findViewById(R.id.etContact)
        btnModifier = findViewById(R.id.btnModifier)
        btnAjouter = findViewById(R.id.btnAjouter)
        lvContacts = findViewById(R.id.lvContacts)

        // Exercice 4 : la liste est triée dès le départ
        trierContacts()

        // Création de l'adaptateur avec un layout Android prédéfini
        // simple_list_item_activated_1 met en surbrillance l'élément sélectionné
        adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_activated_1,
            contacts
        )
        lvContacts.adapter = adapter

        // Clic sur un élément de la liste → on copie son texte dans l'EditText
        lvContacts.setOnItemClickListener { _, _, position, _ ->
            positionSelectionnee = position
            lvContacts.setItemChecked(position, true)
            etContact.setText(contacts[position])
            etContact.setSelection(etContact.text.length) // curseur à la fin
            etContact.requestFocus()
        }

        // Exercice 2 : appui long → suppression après confirmation
        lvContacts.setOnItemLongClickListener { _, _, position, _ ->
            confirmerSuppression(position)
            true // true = l'événement est consommé (pas de clic simple ensuite)
        }

        // Clic sur le bouton "Modifier"
        btnModifier.setOnClickListener { modifierContact() }

        // Exercice 1 : clic sur le bouton "Ajouter"
        btnAjouter.setOnClickListener { ajouterContact() }

        // Bonus : la touche "Terminé" du clavier valide aussi la modification
        etContact.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                modifierContact()
                true
            } else {
                false
            }
        }
    }

    private fun modifierContact() {
        // Vérification 1 : un élément doit être sélectionné
        if (positionSelectionnee == -1) {
            Toast.makeText(this, R.string.msg_aucune_selection, Toast.LENGTH_SHORT).show()
            return
        }

        // Vérification 2 : le texte ne doit pas être vide
        val nouveauNom = lireNom() ?: return

        // Exercice 3 : refuser un nom déjà présent (sauf le contact lui-même)
        if (existeDeja(nouveauNom, positionIgnoree = positionSelectionnee)) {
            etContact.error = getString(R.string.msg_doublon)
            return
        }

        // Mise à jour de la donnée...
        contacts[positionSelectionnee] = nouveauNom
        trierContacts()
        // ...puis on prévient l'adaptateur pour qu'il redessine la liste
        adapter.notifyDataSetChanged()

        // Après le tri, le contact a peut-être changé de place : on le resélectionne
        selectionner(contacts.indexOf(nouveauNom))
        Toast.makeText(this, getString(R.string.msg_modifie, nouveauNom), Toast.LENGTH_SHORT).show()
    }

    // Exercice 1 : ajouter le texte de l'EditText comme nouveau contact
    private fun ajouterContact() {
        val nouveauNom = lireNom() ?: return

        if (existeDeja(nouveauNom, positionIgnoree = -1)) {
            etContact.error = getString(R.string.msg_doublon)
            return
        }

        contacts.add(nouveauNom)
        trierContacts()
        adapter.notifyDataSetChanged()

        // On sélectionne le nouveau contact et on le fait défiler à l'écran
        val position = contacts.indexOf(nouveauNom)
        selectionner(position)
        lvContacts.smoothScrollToPosition(position)
        Toast.makeText(this, getString(R.string.msg_ajoute, nouveauNom), Toast.LENGTH_SHORT).show()
    }

    // Exercice 2 : demander confirmation avant de supprimer
    private fun confirmerSuppression(position: Int) {
        val nom = contacts[position]
        AlertDialog.Builder(this)
            .setTitle(R.string.dlg_supprimer_titre)
            .setMessage(getString(R.string.dlg_supprimer_message, nom))
            .setPositiveButton(R.string.dlg_oui) { _, _ -> supprimerContact(position) }
            .setNegativeButton(R.string.dlg_non, null)
            .show()
    }

    private fun supprimerContact(position: Int) {
        val nom = contacts.removeAt(position)
        adapter.notifyDataSetChanged()

        // La sélection n'est plus valable : on la remet à zéro
        selectionner(-1)
        etContact.text.clear()
        Toast.makeText(this, getString(R.string.msg_supprime, nom), Toast.LENGTH_SHORT).show()
    }

    // Lit le nom saisi ; affiche une erreur et renvoie null s'il est vide
    private fun lireNom(): String? {
        val nom = etContact.text.toString().trim()
        if (nom.isEmpty()) {
            etContact.error = getString(R.string.msg_nom_vide)
            return null
        }
        return nom
    }

    // Exercice 3 : vrai si le nom existe déjà (sans tenir compte des majuscules)
    private fun existeDeja(nom: String, positionIgnoree: Int): Boolean {
        return contacts.withIndex().any { (i, c) ->
            i != positionIgnoree && c.equals(nom, ignoreCase = true)
        }
    }

    // Exercice 4 : tri alphabétique (sans tenir compte des majuscules)
    private fun trierContacts() {
        contacts.sortWith(String.CASE_INSENSITIVE_ORDER)
    }

    // Met à jour la position mémorisée et la surbrillance de la ListView
    private fun selectionner(position: Int) {
        positionSelectionnee = position
        if (position == -1) {
            lvContacts.clearChoices()
            lvContacts.requestLayout() // redessine pour enlever la surbrillance
        } else {
            lvContacts.setItemChecked(position, true)
        }
    }
}
