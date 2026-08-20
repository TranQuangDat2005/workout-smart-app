░▒█▀▀█░▀█▀░▀▀█▀▀░░░▒█▀▀▀░▒█░░░░▒█▀▀▀█░▒█░░▒█

░▒█░▄▄░▒█░░░▒█░░░░░▒█▀▀░░▒█░░░░▒█░░▒█░▒█▒█▒█

░▒█▄▄▀░▄█▄░░▒█░░░░░▒█░░░░▒█▄▄█░▒█▄▄▄█░▒▀▄▀▄▀



===========================================================

\[ĐỊNH NGHĨA CÁC NHÁNH]

* Main: chứa lịch sử rút gọn, chỉ gồm các phiên bản phát hành chính thức (production-ready).Đây là nhánh đại diện cho trạng thái ổn định của dự án, mỗi commit thường được gắn tag phiên bản.
* Develop: chứa toàn bộ lịch sử phát triển, nơi các tính năng mới được merge vào trước khi tạo bản release. Đây là nhánh tích hợp, tập trung tất cả thay đổi từ các nhánh phụ.
* Feature: được tạo từ develop, dùng để phát triển từng tính năng riêng biệt. Khi hoàn thành, chúng được merge trở lại develop.
* Release: được tạo từ develop khi chuẩn bị phát hành. Chỉ dùng để fix bug nhỏ, viết tài liệu, và chuẩn bị release. Khi xong, merge vào cả main và develop. Có thể coi đây là “khu vực chờ” để tinh chỉnh trước khi đưa lên production.
* Hotfix: được tạo từ main khi cần sửa lỗi khẩn cấp trên bản đã phát hành (nằm trên main). Sau khi fix, merge vào cả main và develop. (dành cho bản phát hành mới nhất)
* Support: được dùng để duy trì các phiên bản cũ của phần mềm sau khi đã phát hành, khi bạn vẫn cần vá lỗi hoặc cập nhật nhỏ cho khách hàng đang dùng phiên bản đó. Khác với hotfix/ (vá lỗi khẩn cấp cho bản hiện tại), nhánh support/ cho phép bạn tiếp tục phát triển song song trên một phiên bản đã release, trong khi vẫn có thể phát triển phiên bản mới trên develop





\[LUỒNG TỔNG THỂ CỦA GIT FLOW]

1. Nhánh develop được tạo ra từ main.

2\. Nhánh release được tạo ra từ develop.

3\. Các nhánh feature được tạo ra từ develop.

4\. Khi một tính năng hoàn thành, nó sẽ được merge vào develop.

5\. Khi nhánh release hoàn tất, nó sẽ được merge vào cả develop và main.

6\. Nếu phát hiện lỗi trong main, một nhánh hotfix sẽ được tạo từ main.

7\. Khi hotfix hoàn tất, nó sẽ được merge vào cả develop và main.

===========================================================



\[HƯỚNG DẪN SET UP]

Bước 1: Tải thư viện git flow next: https://github.com/gittower/git-flow-next/releases

Bước 2: Giải nén và chạy exe

Bước 3: Thêm đường dẫn của folder chứa file exe đó vào trong Environment Variables → PATH.

Bước 4: Mở cmd và chạy git flow version -> Nếu ra version thì là cài thành công



\[HƯỚNG DẪN SỬ DỤNG THEO LUỒNG]



*//Luồng khởi tạo//*



Khởi tạo git flow init

**\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_**

**C:\\Users\\daizl\\Desktop\\Save\\zOthers\\Dự án nhà trọ\\apartment-operation-system>git flow init**

**Branch name for production releases \[main]: main**

**Branch name for development \[develop]: develop**

**Feature branch prefix \[feature/]: feature**

**Bugfix branch prefix \[bugfix/]: bugfix**

**Release branch prefix \[release/]: release**

**Hotfix branch prefix \[hotfix/]: hotfix**

**Support branch prefix \[support/]: support**

**Version tag prefix \[]: **

**Git flow has been initialized**

**\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_**





*//Luồng xử lý vòng đời của nhánh feature//*

* Xảy ra khi tạo một tính năng mới (thay tên tính năng vào ***feature\_branch***)



Bắt đầu: checkout develop -> tạo nhánh feature

**\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_**

**git flow feature start *feature\_branch***

**\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_**



Kết thúc: checkout feature -> merge vào develop

**\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_**

**git flow feature finish *feature\_branch***

**\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_**



*//Luồng xử lý vòng đời của nhánh release//*

* xảy ra khi fix bug nhỏ, viết tài liệu chuẩn bị cho release chính thức, không thêm tính năng mới nữa (thay version vào ***0.1.0***)



Bắt đầu: checkout develop -> tạo một nhánh mới tên là release/0.1.0 từ nhánh develop

**\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_**

**git flow release start *0.1.0***

**\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_**



Kết thúc: checkout main -> merge vào main và merge ngược lại vào develop -> nhánh release sẽ bị xóa

**\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_**

**git flow release finish '0.1.0'**

**\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_**



*//Luồng xử lý vòng đời của nhánh hotfix//*

* Xảy ra khi sửa lỗi trực tiếp của phiên bản phát hành mới nhất hiện tại



Bắt đầu: checkout main -> tạo nhánh hotfix

**\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_**

**git flow hotfix start hotfix\_branch**

**\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_**



Kết thúc: checkout main ->  merge vào main -> checkout develop -> merge vào develop -> tạo tag -> rồi xóa nhánh hotfix.

**\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_**

**git flow hotfix finish hotfix\_branch**

**\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_**



//Trường hợp duy trì maintain bản phát hành cũ, quản lý vòng đời của nhánh support

* Xảy ra khi vẫn duy trì maintain bản phát hành cũ cho người dùng cuối (thay phiên bản vào ***1.0.x***)



Bắt đầu: checkout main từ một version cụ thể -> tạo ra nhánh support

**\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_**

**git flow support start *1.0.x***

**\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_**



Kết thúc: checkout main -> merge vào bản mới nhất của dòng phiên bản tại main -> checkout develop -> merge ngược vào develop (vào HEAD) -> Xóa nhánh support

**\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_**

**git flow support finish *1.0.x***

**\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_**



//List

git flow /tên loại nhánh/

\-> VD: git flow support
