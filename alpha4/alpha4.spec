Name:           alpha4
Version:        %{_version}
# use time stamp as build version 
Release:        %(date +"%%Y%%m%%d%%H%%M%%S")
Summary:        Alpha4 P2P Messaging Server
Group:			Applications/System
License:        N/A
Source0:    	%{name}-%{version}.tar.gz
BuildRoot:  	%{_tmppath}/%{name}-%{version}-%{release}-build
BuildArch: 		noarch
Requires:  		java >= 17
Provides:		%{name}

%description
Alpha4 P2P Messaging Server v%{version}

%prep

%setup -q

%build

%install
mkdir -p %{buildroot}/usr/share/%{name}
cp -rf *jar %{buildroot}/usr/share/%{name}
#cp -f server/target/%{name}-%{_ver}.jar %{buildroot}/usr/share/%{name}

%clean
rm -rf %{buildroot}

%files
%defattr(640,root,root,750)
%attr(750, root, root) /usr/share/%{name}/*.jar
#%attr(750, root, root) /usr/share/%{name}/lib/*

%doc

%changelog
* Thu Feb 22 2024 Anton Kalashnikov <anton@kalashnikov.com>; 0.0.1
- First release of %{name}