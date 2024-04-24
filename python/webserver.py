from fastapi import FastAPI, UploadFile, Form
from fastapi.responses import HTMLResponse, FileResponse
from typing import Annotated
import uvicorn
from pathlib import Path
import shutil
from PIL import Image
import io
import cv2
shutil.rmtree("temp") if Path("temp").exists() else None
Path("temp").mkdir( exist_ok=True)
app = FastAPI()
with open("python/home.html", "r") as f:
    homepage = f.read()
#-----------------------------------------#
#change to mods mapimg folder             #
path = 'C:/Users/sacha/mcserver/mapimg'   #
#-----------------------------------------#
vidpath = path+'/vids/'
@app.get("/")
async def home():
    return HTMLResponse(content=homepage, status_code=200)
def getimg(i):
    with open (str(i), "rb") as f:
        img = f.read()
    return img
async def prossessframe(id,count,frame,success):
    cv2.imwrite(f"{vidpath}{str(id)}/{count}.png", cv2.resize(frame, (128, 128), interpolation = cv2.INTER_AREA)) if success else None
@app.post("/upload")
async def upload(id:Annotated[int, Form()],image:UploadFile):
    if image.content_type == 'video/mp4': 
        with open("temp/"+str(id)+".mp4", "wb") as f:
            f.write(await image.read())
        try:
            shutil.rmtree(vidpath+str(id))
        except:
            pass
        Path(vidpath+str(id)).mkdir( exist_ok=True)
        vidObj = cv2.VideoCapture("temp/"+str(id)+".mp4")
        count = 0
        success = 1
        while success: 
            success, frame = vidObj.read() 
            await prossessframe(id,count,frame,success)
            count += 1
        print("prossesed "+str(count)+ " frames for map id "+str(id))
        shutil.rmtree("temp")
        Path("temp").mkdir( exist_ok=True)
        return HTMLResponse(content='<meta http-equiv="refresh" content="0; URL=/" />', status_code=200)
    elif image.content_type == 'image/jpeg' or image.content_type == 'image/jpg' or image.content_type == 'image/png':
        print("got an image")
        img = await image.read()
        img = Image.open(io.BytesIO(img)).resize((128, 128))
        img.verify()
        img.save(f"{path}/images/{id}.png","PNG",optimize=True)
        return HTMLResponse(content='<meta http-equiv="refresh" content="0; URL=/" />', status_code=200)
    else:
        print(image.content_type+" not supported")
        return HTMLResponse(content=image.content_type+" not supported", status_code=415)

@app.get("/bg.jpg")
async def get_bg():
    return FileResponse(f"bg.jpg", media_type="image/jpg")
@app.get("/icon.jpg")
async def get_icon():
    return FileResponse(f"icon.jpg", media_type="image/jpg")
uvicorn.run(app, host="0.0.0.0", port=8000)