const api = {
    harbor: {
        all(callback:any) {
            const harbors = [{
                sqe: 398,
                place: "제주항 국제 여객 터미널",
                lat: "33.52456237850086",
                lng: "126.54371888191963"
            },
            {
                sqe: 399,
                place: "???",
                lat: 32.52456237850091,
                lng: 126.54371888191863
            }
            ]
            callback({success:true, harbors})
        }
    }
}

export default api;