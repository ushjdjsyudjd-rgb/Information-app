package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.UserDatabase
import com.example.data.UserEntity
import com.example.data.UserRepository
import com.example.ui.FormState
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.UserViewModel
import com.example.ui.UserViewModelFactory
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize database, repository, and view model using standard factory
        val database = UserDatabase.getDatabase(this)
        val repository = UserRepository(database.userDao())
        val factory = UserViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[UserViewModel::class.java]

        setContent {
            MyApplicationTheme {
                // Force RTL layout direction since the app is fully Persian (Farsi)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        UserRegistrationApp(viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserRegistrationApp(viewModel: UserViewModel) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val editingUser by viewModel.editingUser.collectAsStateWithLifecycle()
    val usersList by viewModel.usersList.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var userToDelete by remember { mutableStateOf<UserEntity?>(null) }

    // Listen to ViewModel events like Toast messages
    LaunchedEffect(key1 = true) {
        viewModel.snackbarMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "سامانه مدیریت اطلاعات کاربران",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "ذخیره سازی هوشمند در پایگاه داده داخلی (Room)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth >= 600.dp

            if (isWideScreen) {
                // Side-by-side layout for larger screens (Tablets/Foldables/Desktop)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Form Section (Fixed on the left)
                    Box(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                    ) {
                        FormCard(
                            formState = formState,
                            editingUser = editingUser,
                            viewModel = viewModel,
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        )
                    }

                    // Divider
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // User List Section (Scrollable on the right)
                    Column(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SearchAndHeaderSection(
                            searchQuery = searchQuery,
                            onSearchChange = viewModel::onSearchQueryChange,
                            userCount = usersList.size
                        )

                        UserList(
                            users = usersList,
                            onEdit = viewModel::startEditing,
                            onDelete = { userToDelete = it }
                        )
                    }
                }
            } else {
                // Standard Single Column scrollable layout for phones
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    FormCard(
                        formState = formState,
                        editingUser = editingUser,
                        viewModel = viewModel
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    SearchAndHeaderSection(
                        searchQuery = searchQuery,
                        onSearchChange = viewModel::onSearchQueryChange,
                        userCount = usersList.size
                    )

                    // Fix inner height for phone lists to avoid nested scrolling errors
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 300.dp, max = 800.dp)
                    ) {
                        UserList(
                            users = usersList,
                            onEdit = viewModel::startEditing,
                            onDelete = { userToDelete = it }
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog for deleting a user
    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            confirmButton = {
                Button(
                    onClick = {
                        userToDelete?.let { viewModel.deleteUser(it) }
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("بله، حذف شود")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { userToDelete = null }) {
                    Text("انصراف")
                }
            },
            title = {
                Text(
                    text = "حذف کاربر",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "آیا از حذف اطلاعات کاربر «${userToDelete?.firstName} ${userToDelete?.lastName}» مطمئن هستید؟ این عمل غیرقابل بازگشت است.",
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}

@Composable
fun FormCard(
    formState: FormState,
    editingUser: UserEntity?,
    viewModel: UserViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("form_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (editingUser != null) Icons.Default.Edit else Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (editingUser != null) "ویرایش اطلاعات کاربر" else "ورود اطلاعات کاربر جدید",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Text Inputs
            OutlinedTextField(
                value = formState.firstName,
                onValueChange = viewModel::onFirstNameChange,
                label = { Text("نام") },
                placeholder = { Text("مثال: علی") },
                isError = formState.firstNameError != null,
                supportingText = formState.firstNameError?.let { { Text(it) } },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("first_name_input")
            )

            OutlinedTextField(
                value = formState.lastName,
                onValueChange = viewModel::onLastNameChange,
                label = { Text("نام خانوادگی") },
                placeholder = { Text("مثال: رضایی") },
                isError = formState.lastNameError != null,
                supportingText = formState.lastNameError?.let { { Text(it) } },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("last_name_input")
            )

            OutlinedTextField(
                value = formState.nationalCode,
                onValueChange = viewModel::onNationalCodeChange,
                label = { Text("کد ملی") },
                placeholder = { Text("۱۰ رقم، مثال: 0012345678") },
                isError = formState.nationalCodeError != null,
                supportingText = formState.nationalCodeError?.let { { Text(it) } },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("national_code_input")
            )

            OutlinedTextField(
                value = formState.phoneNumber,
                onValueChange = viewModel::onPhoneNumberChange,
                label = { Text("شماره تلفن") },
                placeholder = { Text("مثال: 09123456789") },
                isError = formState.phoneNumberError != null,
                supportingText = formState.phoneNumberError?.let { { Text(it) } },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phone_input")
            )

            OutlinedTextField(
                value = formState.address,
                onValueChange = viewModel::onAddressChange,
                label = { Text("آدرس کامل") },
                placeholder = { Text("مثال: تهران، خیابان ولیعصر، کوچه مریم، پلاک ۱۰") },
                isError = formState.addressError != null,
                supportingText = formState.addressError?.let { { Text(it) } },
                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("address_input")
            )

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = viewModel::saveUser,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("save_button")
                ) {
                    Icon(
                        imageVector = if (editingUser != null) Icons.Default.Done else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (editingUser != null) "اعمال تغییرات" else "ثبت و ذخیره کاربر",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (editingUser != null) {
                    OutlinedButton(
                        onClick = viewModel::cancelEditing,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(0.8f)
                            .height(50.dp)
                            .testTag("cancel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "انصراف", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SearchAndHeaderSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    userCount: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "لیست کاربران ثبت شده",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$userCount کاربر",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("جستجو بر اساس نام، فامیل یا کد ملی...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input")
        )
    }
}

@Composable
fun UserList(
    users: List<UserEntity>,
    onEdit: (UserEntity) -> Unit,
    onDelete: (UserEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (users.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "هیچ کاربری ثبت نشده است!",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
                Text(
                    text = "شما می‌توانید با پر کردن فرم بالا، اطلاعات کاربران جدید را در دیتابیس ثبت و مدیریت کنید.",
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = users,
                key = { it.id }
            ) { user ->
                UserCardItem(
                    user = user,
                    onEdit = { onEdit(user) },
                    onDelete = { onDelete(user) }
                )
            }
        }
    }
}

@Composable
fun UserCardItem(
    user: UserEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_item_${user.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row (Name & Action Buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.firstName.take(1) + user.lastName.take(1),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "${user.firstName} ${user.lastName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Edit and Delete Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .testTag("edit_button_${user.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "ویرایش",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .testTag("delete_button_${user.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Info Grid / Lines
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                InfoRow(
                    icon = Icons.Default.Info,
                    label = "کد ملی",
                    value = user.nationalCode
                )
                InfoRow(
                    icon = Icons.Default.Phone,
                    label = "تلفن",
                    value = user.phoneNumber
                )
                InfoRow(
                    icon = Icons.Default.Home,
                    label = "آدرس",
                    value = user.address
                )
            }
        }
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 1.dp)
        )
        Text(
            text = "$label: ",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            lineHeight = 16.sp
        )
    }
}
