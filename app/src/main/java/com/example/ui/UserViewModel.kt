package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.UserEntity
import com.example.data.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FormState(
    val firstName: String = "",
    val lastName: String = "",
    val nationalCode: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val nationalCodeError: String? = null,
    val phoneNumberError: String? = null,
    val addressError: String? = null
)

class UserViewModel(private val repository: UserRepository) : ViewModel() {

    private val _formState = MutableStateFlow(FormState())
    val formState: StateFlow<FormState> = _formState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _editingUser = MutableStateFlow<UserEntity?>(null)
    val editingUser: StateFlow<UserEntity?> = _editingUser.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // Observe users, automatically filtering whenever search query changes
    @OptIn(ExperimentalCoroutinesApi::class)
    val usersList: StateFlow<List<UserEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allUsers
            } else {
                repository.searchUsers(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onFirstNameChange(value: String) {
        _formState.update { it.copy(firstName = value, firstNameError = null) }
    }

    fun onLastNameChange(value: String) {
        _formState.update { it.copy(lastName = value, lastNameError = null) }
    }

    fun onNationalCodeChange(value: String) {
        // Only allow digits, max 10 characters
        val filtered = value.filter { it.isDigit() }.take(10)
        _formState.update { it.copy(nationalCode = filtered, nationalCodeError = null) }
    }

    fun onPhoneNumberChange(value: String) {
        // Only allow digits, max 11 characters
        val filtered = value.filter { it.isDigit() }.take(11)
        _formState.update { it.copy(phoneNumber = filtered, phoneNumberError = null) }
    }

    fun onAddressChange(value: String) {
        _formState.update { it.copy(address = value, addressError = null) }
    }

    fun onSearchQueryChange(value: String) {
        _searchQuery.value = value
    }

    fun startEditing(user: UserEntity) {
        _editingUser.value = user
        _formState.value = FormState(
            firstName = user.firstName,
            lastName = user.lastName,
            nationalCode = user.nationalCode,
            phoneNumber = user.phoneNumber,
            address = user.address
        )
    }

    fun cancelEditing() {
        _editingUser.value = null
        _formState.value = FormState()
    }

    fun saveUser() {
        if (!validateForm()) {
            viewModelScope.launch {
                _snackbarMessage.emit("لطفاً خطاهای فرم را برطرف کنید.")
            }
            return
        }

        val state = _formState.value
        val currentUser = _editingUser.value

        viewModelScope.launch {
            try {
                if (currentUser != null) {
                    val updatedUser = currentUser.copy(
                        firstName = state.firstName.trim(),
                        lastName = state.lastName.trim(),
                        nationalCode = state.nationalCode.trim(),
                        phoneNumber = state.phoneNumber.trim(),
                        address = state.address.trim(),
                        timestamp = System.currentTimeMillis() // Update timestamp to bring to top
                    )
                    repository.update(updatedUser)
                    _snackbarMessage.emit("اطلاعات کاربر با موفقیت ویرایش شد.")
                    _editingUser.value = null
                } else {
                    val newUser = UserEntity(
                        firstName = state.firstName.trim(),
                        lastName = state.lastName.trim(),
                        nationalCode = state.nationalCode.trim(),
                        phoneNumber = state.phoneNumber.trim(),
                        address = state.address.trim()
                    )
                    repository.insert(newUser)
                    _snackbarMessage.emit("اطلاعات کاربر با موفقیت ذخیره شد.")
                }
                // Clear the form after saving
                _formState.value = FormState()
            } catch (e: Exception) {
                _snackbarMessage.emit("خطا در ذخیره اطلاعات: ${e.localizedMessage}")
            }
        }
    }

    fun deleteUser(user: UserEntity) {
        viewModelScope.launch {
            try {
                repository.delete(user)
                _snackbarMessage.emit("کاربر با موفقیت حذف شد.")
                // If we were editing this user, cancel editing
                if (_editingUser.value?.id == user.id) {
                    cancelEditing()
                }
            } catch (e: Exception) {
                _snackbarMessage.emit("خطا در حذف کاربر: ${e.localizedMessage}")
            }
        }
    }

    private fun validateForm(): Boolean {
        val state = _formState.value
        var isValid = true

        val firstNameErr = if (state.firstName.trim().isBlank()) "نام نمی‌تواند خالی باشد" else null
        val lastNameErr = if (state.lastName.trim().isBlank()) "نام خانوادگی نمی‌تواند خالی باشد" else null
        
        val nationalCodeErr = when {
            state.nationalCode.isBlank() -> "کد ملی نمی‌تواند خالی باشد"
            state.nationalCode.length != 10 -> "کد ملی باید دقیقا ۱۰ رقم باشد"
            !isValidNationalCode(state.nationalCode) -> "کد ملی وارد شده معتبر نیست"
            else -> null
        }

        val phoneNumberErr = when {
            state.phoneNumber.isBlank() -> "شماره تلفن نمی‌تواند خالی باشد"
            state.phoneNumber.length < 8 -> "شماره تلفن نامعتبر است (حداقل ۸ رقم)"
            state.phoneNumber.startsWith("09") && state.phoneNumber.length != 11 -> "شماره موبایل باید ۱۱ رقم باشد"
            else -> null
        }

        val addressErr = if (state.address.trim().isBlank()) "آدرس نمی‌تواند خالی باشد" else null

        if (firstNameErr != null || lastNameErr != null || nationalCodeErr != null || phoneNumberErr != null || addressErr != null) {
            isValid = false
        }

        _formState.update {
            it.copy(
                firstNameError = firstNameErr,
                lastNameError = lastNameErr,
                nationalCodeError = nationalCodeErr,
                phoneNumberError = phoneNumberErr,
                addressError = addressErr
            )
        }

        return isValid
    }

    private fun isValidNationalCode(code: String): Boolean {
        if (code.length != 10 || !code.all { it.isDigit() }) return false
        
        // Check if all digits are the same (invalid national code)
        if (code.toSet().size == 1) return false
        
        val check = code[9].digitToInt()
        val sum = (0..8).sumOf { i -> code[i].digitToInt() * (10 - i) }
        val r = sum % 11
        return (r < 2 && check == r) || (r >= 2 && check == 11 - r)
    }
}

class UserViewModelFactory(private val repository: UserRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
