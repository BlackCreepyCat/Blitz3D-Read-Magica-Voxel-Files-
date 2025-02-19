; (C) 2024 Crepy Cat use as you want! :=

Graphics3D 1920,1080,32,2
SetBuffer BackBuffer()

; Fonction Max
Function Max(a, b, c)
    If a > b And a > c Then Return a
    If b > c Then Return b
    Return c
End Function

; Fonction pour lire 4 caractères en tant que string
Function ReadFourCC$(file)
    Local s$ = Chr(ReadByte(file)) + Chr(ReadByte(file)) + Chr(ReadByte(file)) + Chr(ReadByte(file))
    Return s$
End Function

Type Voxel
    Field x, y, z
    Field colorIndex
End Type

Type PaletteColor
    Field r, g, b
End Type

Global sizeX, sizeY, sizeZ
Global pivot
Global palette.PaletteColor[256]

Function LoadVOX(filename$)
    file = ReadFile(filename$)
    If file = 0 Then RuntimeError "Impossible d'ouvrir le fichier " + filename$

    ; Lire l'en-tête
    id$ = ReadFourCC(file)
    If id$ <> "VOX " Then RuntimeError "Fichier non compatible"

    version = ReadInt(file)  ; Lire la version (ignorée ici)

    While Not Eof(file)
        chunkId$ = ReadFourCC(file)   ; Lire l'ID du chunk
        chunkSize = ReadInt(file)     ; Taille du chunk
        childChunkSize = ReadInt(file) ; Taille des sous-chunks
        
        Select chunkId$
            Case "SIZE"
                sizeX = ReadInt(file)
                sizeY = ReadInt(file)
                sizeZ = ReadInt(file)
            Case "XYZI"
                numVoxels = ReadInt(file)
                For i = 1 To numVoxels
                    voxel.Voxel = New Voxel
                    voxel\x = ReadByte(file)
                    voxel\y = ReadByte(file)
                    voxel\z = ReadByte(file)
                    voxel\colorIndex = ReadByte(file) - 1 ; Index dans la palette (décalage -1 car MagicaVoxel commence à 1)
                Next
            Case "RGBA"
                For i = 0 To 255
                    palette[i] = New PaletteColor
                    palette[i]\r = ReadByte(file)
                    palette[i]\g = ReadByte(file)
                    palette[i]\b = ReadByte(file)
                    ReadByte(file) ; Ignorer le canal alpha
                Next
            Default
                SeekFile file, FilePos(file) + chunkSize
        End Select
    Wend
    CloseFile file
End Function


; ------------------------
; Freelook cam var
; ------------------------
Global MXS#,MYS#
Global Cam_Pitch#
Global Cam_Yaw#
Global Cam_UpDown#
Global Cam_LeftRight#
Global Cam_VelX#
Global Cam_VelZ#


; Charger le fichier VOX
LoadVOX("G:\_GameArt Softs\MagicaVoxel\vox\veh_cab1.vox")

; Création de la scène 3D
camera = CreateCamera()
CameraClsColor camera, 50, 50, 50
CameraRange camera, 0.1, 1000

PositionEntity camera, sizeX / 2, sizeY / 2, -Max(sizeX, sizeY, sizeZ) ;PositionEntity camera, 0,0,-10

light= CreateLight(1)
RotateEntity light, -55,0,0

; Créer un pivot au centre du modèle
pivot = CreatePivot()
PositionEntity pivot,0,0,0

; Faire regarder la caméra vers le pivot
PointEntity camera, pivot

count%=1

; Fonction pour vérifier si l'entité est dans le champ de vision de la caméra
Function IsVisible(entity, camera)
    Local isVisible = EntityInView (entity, camera)
    Return isVisible
End Function

; Lors de la création des cubes
For voxel.Voxel = Each Voxel
    cube = CreateCube()
    ScaleEntity cube, 0.5, 0.5, 0.5
    EntityShininess cube, 0.5

	DebugLog "Voxel created : " + cube + " / " + count
	count=count+1
	
	
    ; Ajuster les axes pour correspondre à MagicaVoxel
    PositionEntity cube, voxel\x - (sizeX / 2), voxel\z - (sizeZ / 2), -(voxel\y - (sizeY / 2))

    EntityParent cube, pivot
    If palette[voxel\colorIndex] <> Null
        EntityColor cube, palette[voxel\colorIndex]\r, palette[voxel\colorIndex]\g, palette[voxel\colorIndex]\b
    Else
        EntityColor cube, 255, 255, 255 ; Blanc par défaut si la couleur est manquante
    EndIf

    ; Cacher les cubes qui ne sont pas visibles
    If Not IsVisible(cube, camera)
        HideEntity cube
    EndIf
Next



; Boucle principale
While Not KeyDown(1)


    ; Parcourir tous les voxels
  ;  For voxel.Voxel = Each Voxel
  ;      cube = GetEntity(voxel\x, voxel\y, voxel\z)  ; Récupérer le cube spécifique

        ; Vérifier si le cube est visible ou non et le cacher/montrer
   ;     If IsVisible(cube, camera)
  ;          ShowEntity cube
  ;      Else
  ;          HideEntity cube
  ;      EndIf
 ;   Next




If MouseHit(2) Then
PointEntity camera, pivot
EndIf

If MouseDown(2) Then

	FreelookSmooth(MouseDown(2),Camera,1.1,0.3,10,0.0)

EndIf

    TurnEntity pivot , 0, 0.5, 0  ; Rotation lente de la caméra
    RenderWorld
    Flip
Wend

;-----------------------------------------------------
; Gestion du freelook douce
;-----------------------------------------------------
Function FreelookSmooth(Action=True,CamEntity,Velocity#=1.1,Speed#=0.5,Damping#=5,Gravity#=0.0)
	MXS#=MouseXSpeed()/1.5
	MYS#=MouseYSpeed()/1.5
	
	If Action=True
		Cam_Pitch#=Cam_Pitch#+MYS#
		Cam_Yaw#=Cam_Yaw#+MXS#
			
		Cam_UpDown#=Cam_UpDown#+((Cam_Pitch#-Cam_UpDown#)/Damping#)
		Cam_LeftRight#=Cam_LeftRight#+((Cam_Yaw#-Cam_LeftRight#)/Damping#)
				
		RotateEntity CamEntity,Cam_UpDown#,-Cam_LeftRight#,0
			
		MoveMouse GraphicsWidth()/2, GraphicsHeight()/2

		If KeyDown(30) Cam_VelX#=Cam_VelX#-Speed# ElseIf KeyDown(32) Cam_VelX#=Cam_VelX#+Speed#
		If KeyDown(31) Cam_VelZ#=Cam_VelZ#-Speed# ElseIf KeyDown(17) Cam_VelZ#=Cam_VelZ#+Speed#
	
		Cam_VelX#=Cam_VelX#/Velocity#
		Cam_VelZ#=Cam_VelZ#/Velocity#

		MoveEntity CamEntity,Cam_VelX#,0,Cam_VelZ#

		; ----------------------------
		; Gestion de la gravité
		; ----------------------------
		TranslateEntity CamEntity,0,Gravity#,0
	EndIf
End Function

End
;~IDEal Editor Parameters:
;~C#Blitz3D