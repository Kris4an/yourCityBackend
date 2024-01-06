package com.example.controllers

import java.util.*

class Validator {
    companion object {
        fun validatePassword(password: String): Boolean {
            if(password.length < 8 || password.length > 256) return false
            var hasLowercase = false
            var hasUppercase = false
            var hasNumber = false
            password.onEach { char ->
                run {
                    if (char in 'a'..'z') hasLowercase = true
                    if (char in 'A'..'Z') hasUppercase = true
                    if (char in '0'..'9') hasNumber = true
                }
            }
            return hasLowercase && hasUppercase && hasNumber
        }
        fun validatePhone(phone: String?): Boolean {
            if(phone.isNullOrEmpty()) return true
            if(phone.length != 10) return false
            if(!phone.startsWith('0')) return false
            phone.onEach { char ->
                run {
                    if (char !in '0'..'9') return false
                }
            }
            return true
        }
        private fun validateEmailPart(part: String):Boolean{
            if(part.isEmpty()) return false
            val allowedSpecialChars = "_-.".toCharArray()
            for(i in part.indices){
                val char = part[i]
                if(!(char in 'a'..'z' || char in '0'..'9')){
                    if(!allowedSpecialChars.contains(char)) return false
                    try{
                        if(allowedSpecialChars.contains(part[i-1]) || allowedSpecialChars.contains(part[i+1])) return false
                    }catch (e: Exception){
                        return false
                    }
                }
            }
            return true
        }
        fun validateEmail(email: String): Boolean {
            if(email.length < 4 || email.length > 35) return false
            email.lowercase(Locale.getDefault())
            val arr = email.split("@").toTypedArray()
            if(arr.size != 2) return false
            return validateEmailPart(arr[0]) && validateEmailPart(arr[1])
        }
        fun validateNonAdminRole(role: String): Boolean {
            val roles = arrayOf("student","citizen")
            return roles.contains(role)
        }
        fun validateName(name: String): Boolean {
            val allowedCharacters = arrayOf('а','б','в','г','д','е','ж','з','и','й','к','л','м','н','о','п','р','с','т','у','ф','х','ц','ч','ш','щ','ъ','ь','ю','я')
            val names = name.split(' ')
            if(name.length !in 1..35) return false
            names.onEach { n ->
                run {
                    n.substring(1).onEach { c ->
                        run {
                            if(c !in allowedCharacters) return false
                        }
                    }
                    if(!n[0].isUpperCase() && n[0] !in allowedCharacters) return false
                }
            }
            return true
        }
    }
}