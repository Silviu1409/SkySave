package com.example.skysave.datatypes


data class User(val uid: String, val email: String, val alias: String, var starred_files: List<String>) : java.io.Serializable