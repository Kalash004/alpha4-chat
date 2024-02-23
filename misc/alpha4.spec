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

%pre
if [ "$1" = 1 ]; then
	echo "Stopping previous service version"
    {
		systemctl stop %{name}
	} || {
		echo "Failed to stop previous service version"
	}
fi

%build

%install
mkdir -p %{buildroot}/usr/share/%{name}
mkdir -p %{buildroot}/etc/systemd/system
cp -f %{name}.service %{buildroot}/etc/systemd/system
cp -f *jar %{buildroot}/usr/share/%{name}
cp -f *.sh %{buildroot}/usr/share/%{name}
cp -f *.properties %{buildroot}/usr/share/%{name}

%files
%defattr(640,root,root,750)
%attr(644, root, root) /usr/share/%{name}
%attr(644, root, root) /usr/share/%{name}/*.jar
%attr(644, root, root) /usr/share/%{name}/*.properties
%attr(755, root, root) /usr/share/%{name}/*.sh
%attr(644, root, root) /etc/systemd/system/%{name}.service
%doc

%clean
rm -rf %{buildroot}

%post
if [ "$1" = 1 ]; then
	# we have different path
	chmod ugo+x /usr/share/%{name}/*.sh
	echo "Refreshing systemd services"
	systemctl daemon-reload
	echo "Starting service"
	systemctl start %{name}
	echo "Enabling boot time start"
	systemctl enable %{name}
fi

%preun
if [ "$1" = 0 ]; then
	echo "Stopping service before removal"
    {
		systemctl stop %{name}
	} || {
		echo "Failed to stop previous service version"
	}
	echo "Refreshing systemd services"
	systemctl daemon-reload
	systemctl reset-failed
fi

%changelog
* Thu Feb 22 2024 Anton Kalashnikov <anton@kalashnikov.com>; 0.0.1
- First release of %{name}