package cn.edu.tju.notepad.network

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET("api/notes")
    suspend fun getNotes(@Query("lastSync") lastSync: Long = 0): Response<NoteResponse>

    @POST("api/notes")
    suspend fun createNote(@Body note: ApiNote): Response<CreateNoteResponse>

    @PUT("api/notes/{id}")
    suspend fun updateNote(@Path("id") id: Long, @Body note: ApiNote): Response<UpdateNoteResponse>

    @DELETE("api/notes/{id}")
    suspend fun deleteNote(@Path("id") id: Long): Response<Map<String, Any>>

    @Multipart
    @POST("api/upload")
    suspend fun uploadImage(@Part image: MultipartBody.Part): Response<UploadResponse>

    @GET("health")
    suspend fun healthCheck(): Response<Map<String, Any>>
}